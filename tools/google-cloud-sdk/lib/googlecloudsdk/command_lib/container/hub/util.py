# -*- coding: utf-8 -*- #
# Copyright 2019 Google LLC. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Utils for GKE Hub commands."""
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import base64
import os
import re
import subprocess
import tempfile
import textwrap

from containerregistry.client import docker_name
from containerregistry.client.v2_2 import docker_image
from googlecloudsdk.api_lib.cloudresourcemanager import projects_api
from googlecloudsdk.api_lib.container import kubeconfig as kconfig
from googlecloudsdk.api_lib.container import util as c_util
from googlecloudsdk.api_lib.container.images import util as i_util
from googlecloudsdk.api_lib.util import apis as core_apis
from googlecloudsdk.api_lib.util import waiter
from googlecloudsdk.calliope import exceptions as calliope_exceptions
from googlecloudsdk.command_lib.projects import util as p_util
from googlecloudsdk.core import exceptions
from googlecloudsdk.core import http
from googlecloudsdk.core import log
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources
from googlecloudsdk.core.console import console_io
from googlecloudsdk.core.util import files
from googlecloudsdk.core.util import times

# The name of the Deployment for the runtime Connect agent.
RUNTIME_CONNECT_AGENT_DEPLOYMENT_NAME = 'gke-connect-agent'

# The app label applied to Pods for the install agent workload.
AGENT_INSTALL_APP_LABEL = 'gke-connect-agent-installer'

# The name of the Connect agent install deployment.
AGENT_INSTALL_DEPLOYMENT_NAME = 'gke-connect-agent-installer'

# The name of the Secret that stores the Google Cloud Service Account
# credentials. This is also the basename of the only key in that secret's Data
# map, the filename '$GCP_SA_KEY_SECRET_NAME.json'.
GCP_SA_KEY_SECRET_NAME = 'creds-gcp'

# The name of the secret that will store the Docker private registry
# credentials, if they are provided.
IMAGE_PULL_SECRET_NAME = 'connect-image-pull-secret'

CONNECT_RESOURCE_LABEL = 'hub.gke.io/project'

MANIFEST_SAVED_MESSAGE = """\
Manifest saved to [{0}]. Please apply the manifest to your cluster with \
`kubectl apply -f {0}`. You must have `cluster-admin` privilege in order to \
deploy the manifest.

**This file contains sensitive data; please treat it with the same discretion \
as your service account key file.**"""

# The manifest used to deploy the Connect agent install workload and its
# supporting components.
#
# Note that the deployment must be last: kubectl apply deploys resources in
# manifest order, and the deployment depends on other resources; and the
# imagePullSecrets template below is appended to this template if image
# pull secrets are required.
INSTALL_MANIFEST_TEMPLATE = """\
apiVersion: v1
kind: Namespace
metadata:
  name: {namespace}
  labels:
    {connect_resource_label}: {project_id}
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-config
  namespace: {namespace}
data:
  project_id: "{project_id}"
  project_number: "{project_number}"
  membership_name: "{membership_name}"
  proxy: "{proxy}"
  image: "{image}"
---
apiVersion: v1
kind: Secret
metadata:
  name: {gcp_sa_key_secret_name}
  namespace: {namespace}
data:
  {gcp_sa_key_secret_name}.json: {gcp_sa_key}
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: {project_id}-gke-connect-agent-role-binding
  labels:
    {connect_resource_label}: {project_id}
subjects:
- kind: ServiceAccount
  name: default
  namespace: {namespace}
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {agent_install_deployment_name}
  namespace: {namespace}
  labels:
    app: {agent_install_app_label}
spec:
  selector:
    matchLabels:
      app: {agent_install_app_label}
  template:
    metadata:
      labels:
        app: {agent_install_app_label}
    spec:
      containers:
      - name: connect-agent-installer
        image: {image}
        command:
          - gkeconnect_bin/bin/gkeconnect_agent
          - --install
          - --sleep-after-install
          - --config
          - user-config
        imagePullPolicy: Always
        env:
        - name: MY_POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace"""

# The secret that will be installed if a Docker registry credential is provided.
# This is appended to the end of INSTALL_MANIFEST_TEMPLATE.
IMAGE_PULL_SECRET_TEMPLATE = """\
apiVersion: v1
kind: Secret
metadata:
  name: {name}
  namespace: {namespace}
  labels:
    {connect_resource_label}: {project_id}
data:
  .dockerconfigjson: {image_pull_secret}
type: kubernetes.io/dockerconfigjson"""

# The CustomResourceDefinition for the Membership Resource. It is created on an
# as needed basis when registering a cluster to the hub.
MEMBERSHIP_CRD_MANIFEST = """\
apiVersion: apiextensions.k8s.io/v1beta1
kind: CustomResourceDefinition
metadata:
  name: memberships.hub.gke.io
spec:
  group: hub.gke.io
  scope: Cluster
  names:
    plural: memberships
    singular: membership
    kind: Membership
  versions:
  - name: v1beta1
    served: true
    storage: true
  validation:
    openAPIV3Schema:
      required:
      - spec
      properties:
        metadata:
          type: object
          properties:
            name:
              type: string
              pattern: '^(membership|test-[a-f0-9\\-]+)$'
        spec:
          type: object
          properties:
            owner:
              type: object
              properties:
                id:
                  type: string
                  description: Membership owner ID. Should be immutable."""

# The Membership Resource that enforces cluster exclusivity. It specifies the
# hub project that the cluster is registered to. During registration, it is used
# to ensure a user does not register a cluster to multiple hub projects.
MEMBERSHIP_CR_TEMPLATE = """\
kind: Membership
apiVersion: hub.gke.io/v1beta1
metadata:
  name: membership
spec:
  owner:
    id: projects/{project_id}"""

AGENT_INSTALL_INITIAL_WAIT_MS = 1000 * 2
AGENT_INSTALL_TIMEOUT_MS = 1000 * 45
AGENT_INSTALL_MAX_POLL_INTERVAL_MS = 1000 * 3
AGENT_INSTALL_INITIAL_POLL_INTERVAL_MS = 1000 * 1

NAMESPACE_DELETION_INITIAL_WAIT_MS = 0
NAMESPACE_DELETION_TIMEOUT_MS = 1000 * 60 * 2
NAMESPACE_DELETION_MAX_POLL_INTERVAL_MS = 1000 * 15
NAMESPACE_DELETION_INITIAL_POLL_INTERVAL_MS = 1000 * 5

# The Connect agent image to use by default.
DEFAULT_CONNECT_AGENT_IMAGE = 'gcr.io/gkeconnect/gkeconnect-gce'
# The Connect agent image tag to use by default.
DEFAULT_CONNECT_AGENT_TAG = 'release'


def AddCommonArgs(parser):
  """Adds the flags shared between 'hub' subcommands to parser.

  Args:
    parser: an argparse.ArgumentParser, to which the common flags will be added
  """
  parser.add_argument(
      '--context',
      required=True,
      type=str,
      help=textwrap.dedent("""\
          The context in the kubeconfig file that specifies the cluster.
        """),
  )
  parser.add_argument(
      '--kubeconfig',
      type=str,
      help=textwrap.dedent("""\
          The kubeconfig file containing an entry for the cluster. Defaults to
          $KUBECONFIG if it is set in the environment, otherwise defaults to
          to $HOME/.kube/config.
        """),
  )
  parser.add_argument(
      '--kubeconfig-file',
      type=str,
      hidden=True,
      help=textwrap.dedent("""\
          The kubeconfig file containing an entry for the cluster. Defaults to
          $KUBECONFIG if it is set in the environment, otherwise defaults to
          to $HOME/.kube/config.
        """),
      dest='kubeconfig',
  )


def UserAccessibleProjectIDSet():
  """Retrieve the project IDs of projects the user can access.

  Returns:
    set of project IDs.
  """
  return set(p.projectId for p in projects_api.List())


def _MembershipClient():
  api_version = core_apis.ResolveVersion('gkehub')
  return core_apis.GetClientInstance('gkehub', api_version)


def _ComputeClient():
  api_version = core_apis.ResolveVersion('compute')
  return core_apis.GetClientInstance('compute', api_version)


def CreateMembership(project, membership_id, description,
                     gke_cluster_self_link):
  """Creates a Membership resource in the GKE Hub API.

  Args:
    project: the project in which to create the membership
    membership_id: the value to use for the membership_id
    description: the value to put in the description field
    gke_cluster_self_link: the selfLink for the cluster if it is a GKE cluster,
      or None if it is not

  Returns:
    the created Membership resource.

  Raises:
    - apitools.base.py.HttpError: if the request returns an HTTP error
    - exceptions raised by waiter.WaitFor()
  """
  client = _MembershipClient()
  messages = client.MESSAGES_MODULE
  request = messages.GkehubProjectsLocationsGlobalMembershipsCreateRequest(
      membership=messages.Membership(description=description),
      parent='projects/{}/locations/global'.format(project),
      membershipId=membership_id,
  )
  if gke_cluster_self_link:
    endpoint = messages.MembershipEndpoint(
        gkeCluster=messages.GkeCluster(resourceLink=gke_cluster_self_link))
    request.membership.endpoint = endpoint

  op = client.projects_locations_global_memberships.Create(request)
  op_resource = resources.REGISTRY.ParseRelativeName(
      op.name, collection='gkehub.projects.locations.operations')
  return waiter.WaitFor(
      waiter.CloudOperationPoller(client.projects_locations_global_memberships,
                                  client.projects_locations_operations),
      op_resource, 'Waiting for membership to be created')


def GetMembership(name):
  """Gets a Membership resource from the GKE Hub API.

  Args:
    name: the full resource name of the membership to get, e.g.,
      projects/foo/locations/global/memberships/name.

  Returns:
    a Membership resource

  Raises:
    apitools.base.py.HttpError: if the request returns an HTTP error
  """

  client = _MembershipClient()
  return client.projects_locations_global_memberships.Get(
      client.MESSAGES_MODULE.GkehubProjectsLocationsGlobalMembershipsGetRequest(
          name=name))


def ProjectForClusterUUID(uuid, projects):
  """Retrieves the project that the cluster UUID has a Membership with.

  Args:
    uuid: the UUID of the cluster.
    projects: sequence of project IDs to consider.

  Returns:
    a project ID.

  Raises:
    apitools.base.py.HttpError: if any request returns an HTTP error
  """

  client = _MembershipClient()
  for project in projects:
    if project:
      parent = 'projects/{}/locations/global'.format(project)
      membership_response = client.projects_locations_global_memberships.List(
          client.MESSAGES_MODULE
          .GkehubProjectsLocationsGlobalMembershipsListRequest(parent=parent))
      for membership in membership_response.resources:
        membership_uuid = _ClusterUUIDForMembershipName(membership.name)
        if membership_uuid == uuid:
          return project
  return None


def _ClusterUUIDForMembershipName(membership_name):
  """Extracts the cluster UUID from the Membership resource name.

  Args:
    membership_name: the full resource name of a membership, e.g.,
      projects/foo/locations/global/memberships/name.

  Returns:
    the name in the membership resource, a cluster UUID.

  Raises:
    exceptions.Error: if the membership was malformed.
  """

  match_membership = 'projects/.+/locations/global/memberships/(.+)'
  matches = re.compile(match_membership).findall(membership_name)
  if len(matches) != 1:
    # This should never happen.
    raise exceptions.Error(
        'unable to parse membership {}'.format(membership_name))
  return matches[0]


def DeleteMembership(name):
  """Deletes a membership from the GKE Hub.

  Args:
    name: the full resource name of the membership to delete, e.g.,
      projects/foo/locations/global/memberships/name.

  Raises:
    apitools.base.py.HttpError: if the request returns an HTTP error
  """

  client = _MembershipClient()
  op = client.projects_locations_global_memberships.Delete(
      client.MESSAGES_MODULE
      .GkehubProjectsLocationsGlobalMembershipsDeleteRequest(name=name))
  op_resource = resources.REGISTRY.ParseRelativeName(
      op.name, collection='gkehub.projects.locations.operations')
  waiter.WaitFor(
      waiter.CloudOperationPollerNoResources(
          client.projects_locations_operations), op_resource,
      'Waiting for membership to be deleted')


def GetClusterUUID(kube_client):
  """Gets the UUID of the kube-system namespace.

  Args:
    kube_client: A KubernetesClient.

  Returns:
    the namespace UID

  Raises:
    exceptions.Error: If the UID cannot be acquired.
    calliope_exceptions.MinimumArgumentException: if a kubeconfig file cannot be
      deduced from the command line flags or environment
  """
  return kube_client.GetNamespaceUID('kube-system')


def ImageDigestForContainerImage(name, tag):
  """Given a container image and tag, returns the digest for that image version.

  Args:
    name: the gcr.io registry name plus the image name
    tag: the image tag

  Returns:
    The digest of the image, or None if there is no such image.

  Raises:
    googlecloudsdk.core.UnsupportedRegistryError: If the path is valid,
      but belongs to an unsupported registry.
    i_util.InvalidImageNameError: If the image name is invalid.
    i_util.TokenRefreshError: If there is an error refreshing credentials
      needed to access the GCR repo.
    i_util.UserRecoverableV2Error: If a user-recoverable error occurs accessing
      the GCR repo.
  """

  def _TaggedImage():
    """Display the fully-qualified name."""
    return '{}:{}'.format(name, tag)

  name = i_util.ValidateRepositoryPath(name)
  with i_util.WrapExpectedDockerlessErrors(name):
    with docker_image.FromRegistry(
        basic_creds=i_util.CredentialProvider(),
        name=docker_name.Tag(_TaggedImage()),
        transport=http.Http()) as r:
      return r.digest()


def GenerateInstallManifest(project_id, namespace, image, sa_key_data,
                            image_pull_secret_data, membership_name, proxy):
  """Generates the contents of the GKE Connect agent install manifest.

  Args:
    project_id: The GCP project identifier.
    namespace: The namespace into which to deploy the Connect agent.
    image: The container image to use in the Connect agent install deployment
      (and, later, runtime deployment).
    sa_key_data: The contents of a GCP SA keyfile, base64-encoded.
    image_pull_secret_data: The contents of a secret that will be used as an
      image pull secret for the provided Docker image.
    membership_name: The name of the membership that this manifest is being
      generated for.
    proxy: The HTTP proxy that the agent should use, in the form
      http[s]://<proxy>

  Returns:
    A tuple, containing (
      a string, a YAML manifest that can be used to install the agent,
      the name of the Connect agent install Deployment
    )
  """
  project_number = p_util.GetProjectNumber(project_id)

  install_manifest = INSTALL_MANIFEST_TEMPLATE.format(
      namespace=namespace,
      connect_resource_label=CONNECT_RESOURCE_LABEL,
      project_id=project_id,
      project_number=project_number,
      membership_name=membership_name or '',
      proxy=proxy or '',
      image=image,
      gcp_sa_key=sa_key_data,
      gcp_sa_key_secret_name=GCP_SA_KEY_SECRET_NAME,
      agent_install_deployment_name=AGENT_INSTALL_DEPLOYMENT_NAME,
      agent_install_app_label=AGENT_INSTALL_APP_LABEL)

  if image_pull_secret_data:
    # The indentation of this string literal is important: it must be
    # appendable to the bottom of the deployment_manifest.
    image_pull_secret_section = """\
      imagePullSecrets:
        - name: {}""".format(IMAGE_PULL_SECRET_NAME)

    install_manifest = '{}\n{}\n---\n{}'.format(
        install_manifest, image_pull_secret_section,
        IMAGE_PULL_SECRET_TEMPLATE.format(
            name=IMAGE_PULL_SECRET_NAME,
            namespace=namespace,
            connect_resource_label=CONNECT_RESOURCE_LABEL,
            project_id=project_id,
            image_pull_secret=image_pull_secret_data))

  return install_manifest, AGENT_INSTALL_DEPLOYMENT_NAME


def Base64EncodedFileContents(filename):
  """Reads the provided file, and returns its contents, base64-encoded.

  Args:
    filename: The path to the file, absolute or relative to the current working
      directory.

  Returns:
    A string, the contents of filename, base64-encoded.

  Raises:
   files.Error: if the file cannot be read.
  """
  return base64.b64encode(
      files.ReadBinaryFileContents(files.ExpandHomeDir(filename)))


def DeployConnectAgent(args,
                       service_account_key_data,
                       docker_credential_data,
                       upgrade=False):
  """Deploys the GKE Connect agent to the cluster.

  Args:
    args: arguments of the command.
    service_account_key_data: The contents of a Google IAM service account JSON
      file
    docker_credential_data: A credential that can be used to access Docker, to
      be stored in a secret and referenced from pod.spec.ImagePullSecrets.
    upgrade: whether to attempt to upgrade the agent, rather than replacing it.

  Raises:
    exceptions.Error: If the agent cannot be deployed properly
    calliope_exceptions.MinimumArgumentException: If the agent cannot be
    deployed properly
  """
  kube_client = KubernetesClient(args)

  image = args.docker_image
  if not image:
    # Get the SHA for the default image.
    try:
      digest = ImageDigestForContainerImage(DEFAULT_CONNECT_AGENT_IMAGE,
                                            DEFAULT_CONNECT_AGENT_TAG)
      image = '{}@{}'.format(DEFAULT_CONNECT_AGENT_IMAGE, digest)
    except Exception as exp:
      raise exceptions.Error(
          'could not determine image digest for {}:{}: {}'.format(
              DEFAULT_CONNECT_AGENT_IMAGE, DEFAULT_CONNECT_AGENT_TAG, exp))

  project_id = properties.VALUES.core.project.GetOrFail()
  namespace = _GKEConnectNamespace(kube_client, project_id)

  full_manifest, agent_install_deployment_name = GenerateInstallManifest(
      project_id, namespace, image, service_account_key_data,
      docker_credential_data, args.CLUSTER_NAME, args.proxy)

  # Generate a manifest file if necessary.
  if args.manifest_output_file:
    try:
      files.WriteFileContents(
          files.ExpandHomeDir(args.manifest_output_file),
          full_manifest,
          private=True)
    except files.Error as e:
      exceptions.Error('could not create manifest file: {}'.format(e))

    log.status.Print(MANIFEST_SAVED_MESSAGE.format(args.manifest_output_file))
    return

  log.status.Print('Deploying GKE Connect agent to cluster...')

  # During an upgrade, the namespace should not be deleted.
  if not upgrade:
    # Delete the ns if necessary
    if kube_client.NamespaceExists(namespace):
      console_io.PromptContinue(
          message='Namespace [{namespace}] already exists in the cluster. This '
          'may be from a previous installation of the agent. If you want to '
          'investigate, enter "n" and run\n\n'
          '  kubectl \\\n'
          '    --kubeconfig={kubeconfig} \\\n'
          '    --context={context} \\\n'
          '    get all -n {namespace}\n\n'
          'Continuing will delete namespace [{namespace}].'.format(
              namespace=namespace,
              kubeconfig=kube_client.kubeconfig,
              context=kube_client.context),
          cancel_on_no=True)
      try:
        succeeded, error = waiter.WaitFor(
            KubernetesPoller(),
            NamespaceDeleteOperation(namespace, kube_client),
            'Deleting namespace [{}] in the cluster'.format(namespace),
            pre_start_sleep_ms=NAMESPACE_DELETION_INITIAL_WAIT_MS,
            max_wait_ms=NAMESPACE_DELETION_TIMEOUT_MS,
            wait_ceiling_ms=NAMESPACE_DELETION_MAX_POLL_INTERVAL_MS,
            sleep_ms=NAMESPACE_DELETION_INITIAL_POLL_INTERVAL_MS)
      except waiter.TimeoutError as e:
        # waiter.TimeoutError assumes that the operation is a Google API
        # operation, and prints a debugging string to that effect.
        raise exceptions.Error(
            'Could not delete namespace [{}] from cluster.'.format(namespace))

      if not succeeded:
        raise exceptions.Error(
            'Could not delete namespace [{}] from cluster. Error: {}'.format(
                namespace, error))

  # Create or update the agent install deployment and related resources.
  err = kube_client.Apply(full_manifest)
  if err:
    raise exceptions.Error(
        'Failed to apply manifest to cluster: {}'.format(err))

  kubectl_log_cmd = (
      'kubectl --kubeconfig={} --context={} logs -n {} -l app={}'.format(
          kube_client.kubeconfig, kube_client.context, namespace,
          AGENT_INSTALL_APP_LABEL))

  def _WriteAgentLogs():
    """Writes logs from the agent install deployment to a temporary file."""
    logs, err = kube_client.Logs(
        namespace, 'deployment/{}'.format(agent_install_deployment_name))
    if err:
      log.warning(
          'Could not fetch Connect agent installation deployment logs: {}'
          .format(err))
      return

    _, tmp_file = tempfile.mkstemp(
        suffix='_{}.log'.format(times.Now().strftime('%Y%m%d-%H%M%S')),
        prefix='gke_connect_',
    )
    files.WriteFileContents(tmp_file, logs, private=True)
    log.status.Print(
        'Connect agent installation deployment logs saved to [{}]'.format(
            tmp_file))

  try:
    succeeded, error = waiter.WaitFor(
        KubernetesPoller(),
        DeploymentPodsAvailableOperation(namespace,
                                         RUNTIME_CONNECT_AGENT_DEPLOYMENT_NAME,
                                         image, kube_client),
        'Waiting for Connect agent to be installed',
        pre_start_sleep_ms=AGENT_INSTALL_INITIAL_WAIT_MS,
        max_wait_ms=AGENT_INSTALL_TIMEOUT_MS,
        wait_ceiling_ms=AGENT_INSTALL_MAX_POLL_INTERVAL_MS,
        sleep_ms=AGENT_INSTALL_INITIAL_POLL_INTERVAL_MS)
  except waiter.TimeoutError:
    # waiter.TimeoutError assumes that the operation is a Google API operation,
    # and prints a debugging string to that effect.
    _WriteAgentLogs()
    raise exceptions.Error(
        'Connect agent installation timed out. Leaving deployment in cluster '
        'for further debugging.\nTo view logs from the cluster:\n\n'
        '{}\n'.format(kubectl_log_cmd))

  _WriteAgentLogs()

  if not succeeded:
    raise exceptions.Error(
        'Connect agent installation did not succeed. To view logs from the '
        'cluster: {}\nKubectl error log: {}'.format(kubectl_log_cmd, error))

  log.status.Print('Connect agent installation succeeded.')


class NamespaceDeleteOperation(object):
  """An operation that waits for a namespace to be deleted."""

  def __init__(self, namespace, kube_client):
    self.namespace = namespace
    self.kube_client = kube_client
    self.done = False
    self.succeeded = False
    self.error = None

  def __str__(self):
    return '<deleting namespce {}>'.format(self.namespace)

  def Update(self):
    """Updates this operation with the latest namespace deletion status."""
    err = self.kube_client.DeleteNamespace(self.namespace)

    # The first delete request should succeed.
    if not err:
      return

    # If deletion is successful, the delete command will return a NotFound
    # error.
    if 'NotFound' in err:
      self.done = True
      self.succeeded = True
    else:
      self.error = err


class DeploymentPodsAvailableOperation(object):
  """An operation that tracks whether a Deployment's Pods are all available."""

  def __init__(self, namespace, deployment_name, image, kube_client):
    self.namespace = namespace
    self.deployment_name = deployment_name
    self.image = image
    self.kube_client = kube_client
    self.done = False
    self.succeeded = False
    self.error = None

  def __str__(self):
    return '<Pod availability for {}/{}>'.format(self.namespace,
                                                 self.deployment_name)

  def Update(self):
    """Updates this operation with the latest Deployment availability status."""
    deployment_resource = 'deployment/{}'.format(self.deployment_name)

    def _HandleErr(err):
      """Updates the operation for the provided error."""
      # If the deployment hasn't been created yet, then wait for it to be.
      if 'NotFound' in err:
        return

      # Otherwise, fail the operation.
      self.done = True
      self.succeeded = False
      self.error = err

    # Ensure that the Deployment has the correct image, so that this operation
    # is tracking the status of a new rollout, not the pre-rollout steady state.
    # TODO(b/135121228): Check the generation vs observedGeneration as well.
    deployment_image, err = self.kube_client.GetResourceField(
        self.namespace, deployment_resource,
        '.spec.template.spec.containers[0].image')
    if err:
      _HandleErr(err)
      return
    if deployment_image != self.image:
      return

    spec_replicas, err = self.kube_client.GetResourceField(
        self.namespace, deployment_resource, '.spec.replicas')
    if err:
      _HandleErr(err)
      return

    status_replicas, err = self.kube_client.GetResourceField(
        self.namespace, deployment_resource, '.status.replicas')
    if err:
      _HandleErr(err)
      return

    available_replicas, err = self.kube_client.GetResourceField(
        self.namespace, deployment_resource, '.status.availableReplicas')
    if err:
      _HandleErr(err)
      return

    updated_replicas, err = self.kube_client.GetResourceField(
        self.namespace, deployment_resource, '.status.updatedReplicas')
    if err:
      _HandleErr(err)
      return

    # This mirrors the replica-count logic used by kubectl rollout status:
    # https://github.com/kubernetes/kubernetes/blob/master/pkg/kubectl/rollout_status.go
    # Not enough replicas are up-to-date.
    if updated_replicas < spec_replicas:
      return
    # Replicas of an older version have not been turned down.
    if status_replicas > updated_replicas:
      return
    # Not enough replicas are up and healthy.
    if available_replicas < updated_replicas:
      return

    self.succeeded = True
    self.done = True


class KubernetesPoller(waiter.OperationPoller):
  """An OperationPoller that polls operations targeting Kubernetes clusters."""

  def IsDone(self, operation):
    return operation.done

  def Poll(self, operation_ref):
    operation_ref.Update()
    return operation_ref

  def GetResult(self, operation):
    return (operation.succeeded, operation.error)


class KubernetesClient(object):
  """A client for accessing a subset of the Kubernetes API."""

  def __init__(self, flags):
    """Constructor for KubernetesClient.

    Args:
      flags: the flags passed to the enclosing command

    Raises:
      exceptions.Error: if the client cannot be configured
      calliope_exceptions.MinimumArgumentException: if a kubeconfig file
        cannot be deduced from the command line flags or environment
    """
    self.kubectl_timeout = '20s'

    processor = KubeconfigProcessor()
    self.kubeconfig, self.context = processor.GetKubeconfigAndContext(flags)

  def GetNamespaceUID(self, namespace):
    cmd = ['get', 'namespace', namespace, '-o', 'jsonpath=\'{.metadata.uid}\'']
    out, err = self._RunKubectl(cmd, None)
    if err:
      raise exceptions.Error(
          'Failed to get the UID of the cluster: {}'.format(err))

    return out.replace("'", '')

  def NamespacesWithLabelSelector(self, label):
    cmd = ['get', 'namespace', '-l', label, '-o', 'jsonpath={.metadata.name}']
    out, err = self._RunKubectl(cmd, None)
    if err:
      raise exceptions.Error(
          'Failed to list namespaces in the cluster: {}'.format(err))
    return out.strip().split(' ') if out else []

  def DeleteMembership(self):
    _, err = self._RunKubectl(['delete', 'membership', 'membership'])
    return err

  def _MembershipCRDExists(self):
    cmd = ['get', 'crds', 'memberships.hub.gke.io']
    _, err = self._RunKubectl(cmd, None)
    if err:
      if 'NotFound' in err:
        return False
      raise exceptions.Error('Error retrieving Membership CRD: {}'.format(err))
    return True

  def GetMembershipOwnerID(self):
    """Looks up the owner id field in the Membership resource."""
    if not self._MembershipCRDExists():
      return None

    cmd = ['get', 'membership', 'membership', '-o', 'jsonpath={.spec.owner.id}']
    out, err = self._RunKubectl(cmd, None)
    if err:
      if 'NotFound' in err:
        return None
      raise exceptions.Error('Error retrieving membership id: {}'.format(err))
    return out

  def ApplyMembership(self, membership_cr_manifest):
    # We need to apply the CRD before the resource. `kubectl apply` does not
    # handle ordering CR and CRD creations.
    for manifest in [MEMBERSHIP_CRD_MANIFEST, membership_cr_manifest]:
      err = self.Apply(manifest)
      if err:
        raise exceptions.Error(
            'Failed to apply Membership manifest to cluster: {}'.format(err))

  def NamespaceExists(self, namespace):
    _, err = self._RunKubectl(['get', 'namespace', namespace])
    return err is None

  def DeleteNamespace(self, namespace):
    _, err = self._RunKubectl(['delete', 'namespace', namespace])
    return err

  def GetResourceField(self, namespace, resource, json_path):
    """Returns the value of a field on a Kubernetes resource.

    Args:
      namespace: the namespace of the resource, or None if this resource is
        cluster-scoped
      resource: the resource, in the format <resourceType>/<name>; e.g.,
        'configmap/foo', or <resourceType> for a list of resources
      json_path: the JSONPath expression to filter with

    Returns:
      The field value (which could be empty if there is no such field), or
      the error printed by the command if there is an error.
    """
    cmd = ['-n', namespace] if namespace else []
    cmd.extend(['get', resource, '-o', 'jsonpath={{{}}}'.format(json_path)])
    return self._RunKubectl(cmd)

  def Apply(self, manifest):
    _, err = self._RunKubectl(['apply', '-f', '-'], stdin=manifest)
    return err

  def Logs(self, namespace, log_target):
    """Gets logs from a workload in the cluster.

    Args:
      namespace: the namespace from which to collect logs.
      log_target: the target for the logs command. Any target supported by
        'kubectl logs' is supported here.

    Returns:
      The logs, or an error if there was an error gathering these logs.
    """
    return self._RunKubectl(['logs', '-n', namespace, log_target])

  def _RunKubectl(self, args, stdin=None):
    """Runs a kubectl command with the cluster referenced by this client.

    Args:
      args: command line arguments to pass to kubectl
      stdin: text to be passed to kubectl via stdin

    Returns:
      The contents of stdout if the return code is 0, stderr (or a fabricated
      error if stderr is empty) otherwise
    """
    cmd = [
        c_util.CheckKubectlInstalled(), '--context', self.context,
        '--kubeconfig', self.kubeconfig, '--request-timeout',
        self.kubectl_timeout
    ]
    cmd.extend(args)

    p = subprocess.Popen(
        cmd,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE)
    out, err = p.communicate(stdin)

    if p.returncode != 0 and not err:
      err = 'kubectl exited with return code {}'.format(p.returncode)

    return out if p.returncode == 0 else None, err if p.returncode != 0 else None


class KubeconfigProcessor(object):
  """A helper class that processes kubeconfig and context arguments."""

  def __init__(self):
    """Constructor for KubeconfigProcessor.

    Raises:
      exceptions.Error: if kubectl is not installed
    """
    # Warn if kubectl is not installed.
    if not c_util.CheckKubectlInstalled():
      raise exceptions.Error('kubectl not installed.')

  def GetKubeconfigAndContext(self, flags):
    """Gets the kubeconfig and cluster context from arguments and defaults.

    Args:
      flags: the flags passed to the enclosing command. It must include
        kubeconfig and context.

    Returns:
      the kubeconfig filepath and context name

    Raises:
      calliope_exceptions.MinimumArgumentException: if a kubeconfig file cannot
        be deduced from the command line flags or environment
      exceptions.Error: if the context does not exist in the deduced kubeconfig
        file
    """
    kubeconfig_file = (
        flags.kubeconfig or os.getenv('KUBECONFIG') or '~/.kube/config')
    kubeconfig = files.ExpandHomeDir(kubeconfig_file)
    if not kubeconfig:
      raise calliope_exceptions.MinimumArgumentException(
          ['--kubeconfig'],
          'Please specify --kubeconfig, set the $KUBECONFIG environment '
          'variable, or ensure that $HOME/.kube/config exists')
    kc = kconfig.Kubeconfig.LoadFromFile(kubeconfig)

    context_name = flags.context

    if context_name not in kc.contexts:
      raise exceptions.Error(
          'context [{}] does not exist in kubeconfig [{}]'.format(
              context_name, kubeconfig))

    return kubeconfig, context_name


def DeleteConnectNamespace(args):
  """Delete the namespace in the cluster that contains the connect agent.

  Args:
    args: an argparse namespace. All arguments that were provided to this
      command invocation.

  Raises:
    calliope_exceptions.MinimumArgumentException: if a kubeconfig file cannot
      be deduced from the command line flags or environment
  """

  kube_client = KubernetesClient(args)
  namespace = _GKEConnectNamespace(kube_client,
                                   properties.VALUES.core.project.GetOrFail())
  cleanup_msg = 'Please delete namespace {} manually in your cluster.'.format(
      namespace)

  err = kube_client.DeleteNamespace(namespace)
  if err:
    if 'NotFound' in err:
      # If the namespace was not found, then do not log an error.
      log.status.Print(
          'Namespace [{}] (for context [{}]) did not exist, so it did not '
          'require deletion.'.format(namespace, args.context))
      return
    log.warning(
        'Failed to delete namespace [{}] (for context [{}]): {}. {}'.format(
            namespace, args.context, err, cleanup_msg))
    return


def _GKEConnectNamespace(kube_client, project_id):
  """Returns the namespace into which to install or update the connect agent.

  Connect namespaces are identified by the presence of the hub.gke.io/project
  label. If there is one existing namespace with this label in the cluster, its
  name is returned; otherwise, a connect agent namespace with the project
  number as a suffix is returned. If there are multiple namespaces with the
  hub.gke.io/project label, an error is raised.

  Args:
    kube_client: a KubernetesClient
    project_id: A GCP project identifier

  Returns:
    a string, the namespace

  Raises:
    exceptions.Error: if there are multiple Connect namespaces in the cluster
  """
  selector = '{}={}'.format(CONNECT_RESOURCE_LABEL, project_id)
  namespaces = kube_client.NamespacesWithLabelSelector(selector)
  if not namespaces:
    return 'gke-connect-{}'.format(p_util.GetProjectNumber(project_id))
  if len(namespaces) == 1:
    return namespaces[0]
  raise exceptions.Error(
      'Multiple GKE Connect namespaces in cluster: {}'.format(namespaces))


def GetMembershipCROwnerID(kube_client):
  """Returns the project id of the hub the cluster is a member of.

  The Membership Custom Resource stores the project id of the hub the cluster
  is registered to in the `.spec.owner.id` field.

  Args:
    kube_client: A KubernetesClient.

  Returns:
    a string, the project id
    None, if the Membership CRD or CR do not exist on the cluster.

  Raises:
    exceptions.Error: if the Membership resource does not have a valid owner id
  """

  owner_id = kube_client.GetMembershipOwnerID()
  if owner_id is None:
    return None
  id_prefix = 'projects/'
  if not owner_id.startswith(id_prefix):
    raise exceptions.Error(
        'Membership .spec.owner.id is invalid: {}'.format(owner_id))
  return owner_id[len(id_prefix):]


def ApplyMembershipResources(kube_client, project):
  """Creates or updates the Membership CRD and CR with the hub project id.

  Args:
    kube_client: A KubernetesClient.
    project: The project id of the hub the cluster is a member of.

  Raises:
    exceptions.Error: if the Membership CR or CRD couldn't be applied.
  """

  membership_cr_manifest = MEMBERSHIP_CR_TEMPLATE.format(project_id=project)
  kube_client.ApplyMembership(membership_cr_manifest)


def DeleteMembershipResources(kube_client):
  """Deletes the Membership CRD.

  Due to garbage collection all Membership resources will also be deleted.

  Args:
    kube_client: A KubernetesClient.
  """

  err = kube_client.DeleteMembership()
  if err:
    if 'NotFound' in err:
      # If the Membership resources were not found, then do not log an error.
      log.status.Print(
          'Membership for context [{}]) did not exist, so it did not '
          'require deletion.'.format(kube_client.context))
      return
    log.warning(
        'Failed to delete membership (for context [{}]): {}. '
        'Please delete the membership resource, manually in your cluster:\n\n'
        '  kubectl delete membership membership'.format(kube_client.context,
                                                        err))


def ReleaseTrackCommandPrefix(release_track):
  """Returns a prefix to add to a gcloud command.

  This is meant for formatting an example string, such as:
    gcloud {}container hub register-cluster

  Args:
    release_track: A ReleaseTrack

  Returns:
   a prefix to add to a gcloud based on the release track
  """

  prefix = release_track.prefix
  return prefix + ' ' if prefix else ''


def GKEClusterSelfLink(args):
  """Returns the selfLink of a cluster, if it is a GKE cluster.

  There is no straightforward way to obtain this information from the cluster
  API server directly. This method uses metadata on the Kubernetes nodes to
  determine the instance ID and project ID of a GCE VM, whose metadata is used
  to find the location of the cluster and its name.

  Args:
    args: an argparse namespace. All arguments that were provided to the command
      invocation.

  Returns:
    the full OnePlatform resource path of a GKE cluster, e.g.,
    //container.googleapis.com/project/p/location/l/cluster/c. If the cluster is
    not a GKE cluster, returns None.

  Raises:
    exceptions.Error: if there is an error fetching metadata from the cluster
      nodes
    calliope_exceptions.MinimumArgumentException: if a kubeconfig file
      cannot be deduced from the command line flags or environment
    <others?>
  """

  kube_client = KubernetesClient(args)

  # Get the instance ID and provider ID of some VM. Since all of the VMs should
  # have the same cluster name, arbitrarily choose the first one that is
  # returned from kubectl.

  # The instance ID field is unique to GKE clusters: Kubernetes-on-GCE clusters
  # do not have this field.
  vm_instance_id, err = kube_client.GetResourceField(
      None, 'nodes',
      '.items[0].metadata.annotations.container\\.googleapis\\.com/instance_id')
  if err:
    raise exceptions.Error(
        'Error retrieving instance ID for cluster node: {}'.format(err))
  if not vm_instance_id:
    return None

  # The provider ID field exists on both GKE-on-GCP and Kubernetes-on-GCP
  # clusters. Therefore, even though it contains all of the necessary
  # information, it's presence does not guarantee that this is a GKE cluster.
  vm_provider_id, err = kube_client.GetResourceField(
      None, 'nodes', '.items[0].spec.providerID')
  if err or not vm_provider_id:
    raise exceptions.Error(
        'Error retrieving VM provider ID for cluster node: {}'.format(
            err or 'field does not exist on object'))

  # Parse the providerID to determine the project ID and VM zone.
  matches = re.match(r'^gce://([^/]+?)/([^/]+?)/.+', vm_provider_id)
  if not matches or matches.lastindex != 2:
    raise exceptions.Error(
        'Error parsing project ID and VM zone from provider ID: unexpected format "{}" for provider ID'
        .format(vm_provider_id))
  project_id = matches.group(1)
  vm_zone = matches.group(2)

  # Call the compute API to get the VM instance with this instance ID.
  compute_client = _ComputeClient()
  request = compute_client.MESSAGES_MODULE.ComputeInstancesGetRequest(
      instance=vm_instance_id, project=project_id, zone=vm_zone)
  instance = compute_client.instances.Get(request)
  if not instance:
    raise exceptions.Error('Empty GCE instance returned from compute API.')
  if not instance.metadata:
    raise exceptions.Error(
        'GCE instance with empty metadata returned from compute API.')

  # Read the cluster name and location from the VM instance's metadata.

  # Convert the metadata message to a Python dict.
  metadata = {}
  for item in instance.metadata.items:
    metadata[item.key] = item.value

  cluster_name = metadata.get('cluster-name')
  cluster_location = metadata.get('cluster-location')

  if not cluster_name:
    raise exceptions.Error('Could not determine cluster name from instance.')
  if not cluster_location:
    raise exceptions.Error(
        'Could not determine cluster location from instance.')

  return '//container.googleapis.com/projects/{}/locations/{}/clusters/{}'.format(
      project_id, cluster_location, cluster_name)
