# -*- coding: utf-8 -*- #
# Copyright 2015 Google LLC. All Rights Reserved.
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
"""Api client adapter containers commands."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import os
import time

from apitools.base.py import exceptions as apitools_exceptions
from apitools.base.py import http_wrapper

from googlecloudsdk.api_lib.compute import constants
from googlecloudsdk.api_lib.container import constants as gke_constants
from googlecloudsdk.api_lib.container import util
from googlecloudsdk.api_lib.util import apis as core_apis
from googlecloudsdk.calliope import exceptions
from googlecloudsdk.core import log
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources as cloud_resources
from googlecloudsdk.core import yaml
from googlecloudsdk.core.console import progress_tracker
import six
from six.moves import range  # pylint: disable=redefined-builtin
import six.moves.http_client

WRONG_ZONE_ERROR_MSG = """\
{error}
Could not find [{name}] in [{wrong_zone}].
Did you mean [{name}] in [{zone}]?"""

NO_SUCH_CLUSTER_ERROR_MSG = """\
{error}
No cluster named '{name}' in {project}."""

NO_SUCH_NODE_POOL_ERROR_MSG = """\
No node pool named '{name}' in {cluster}."""

NO_NODE_POOL_SELECTED_ERROR_MSG = """\
Please specify one of the following node pools:
"""

MISMATCH_AUTHORIZED_NETWORKS_ERROR_MSG = """\
Cannot use --master-authorized-networks \
if --enable-master-authorized-networks is not \
specified."""

NO_AUTOPROVISIONING_MSG = """\
Node autoprovisioning is currently in beta.
"""

NO_AUTOPROVISIONING_LIMITS_ERROR_MSG = """\
Must specify both --max-cpu and --max-memory to enable autoprovisioning.
"""

ATMOST_ONE_IDENTITY_FOR_AUTOPROVISIONING_ERROR_MSG = """\
At most one of --autoprovisioning-service-account and \
--autoprovisioning-scopes \
can be specified"""

LIMITS_WITHOUT_AUTOPROVISIONING_MSG = """\
Must enable node autoprovisioning to specify resource limits for autoscaling.
"""

IDENTITY_DEFAULTS_WITHOUT_AUTOPROVISIONING_MSG = """\
Must enable node autoprovisioning to specify default service account or scopes \
for node autoprovisioning
"""

LIMITS_WITHOUT_AUTOPROVISIONING_FLAG_MSG = """\
Must specify --enable-autoprovisioning to specify resource limits for autoscaling.
"""

MISMATCH_ACCELERATOR_TYPE_LIMITS_ERROR_MSG = """\
Maximum and minimum accelerator limits must be set on the same accelerator type.
"""

UNKNOWN_AUTOSCALING_PROFILE_MSG = """\
Unknown autoscaling profile: '{profile}'.
"""

NO_SUCH_LABEL_ERROR_MSG = """\
No label named '{name}' found on cluster '{cluster}'."""

NO_LABELS_ON_CLUSTER_ERROR_MSG = """\
Cluster '{cluster}' has no labels to remove."""

CREATE_SUBNETWORK_INVALID_KEY_ERROR_MSG = """\
Invalid key '{key}' for --create-subnetwork (must be one of 'name', 'range').
"""

CREATE_SUBNETWORK_WITH_SUBNETWORK_ERROR_MSG = """\
Cannot specify both --subnetwork and --create-subnetwork at the same time.
"""

NODE_TAINT_INCORRECT_FORMAT_ERROR_MSG = """\
Invalid value [{key}={value}] for argument --node-taints. Node taint is of format key=value:effect
"""

NODE_TAINT_INCORRECT_EFFECT_ERROR_MSG = """\
Invalid taint effect [{effect}] for argument --node-taints. Valid effect values are NoSchedule, PreferNoSchedule, NoExecute'
"""

LOCAL_SSD_INCORRECT_FORMAT_ERROR_MSG = """\
Invalid local SSD format [{err_format}] for argument --local-ssd-volumes. Valid formats are fs, block
"""

UNKNOWN_WORKLOAD_METADATA_FROM_NODE_ERROR_MSG = """\
Invalid option '{option}' for '--workload-metadata-from-node' (must be one of 'unspecified', 'secure', 'exposed').
"""

ALLOW_ROUTE_OVERLAP_WITHOUT_CLUSTER_CIDR_ERROR_MSG = """\
Flag --cluster-ipv4-cidr must be fully specified (e.g. `10.96.0.0/14`, but not `/14`) with --allow-route-overlap.
"""

ALLOW_ROUTE_OVERLAP_WITHOUT_SERVICES_CIDR_ERROR_MSG = """\
Flag --services-ipv4-cidr must be fully specified (e.g. `10.96.0.0/14`, but not `/14`) with --allow-route-overlap.
"""

PREREQUISITE_OPTION_ERROR_MSG = """\
Cannot specify --{opt} without --{prerequisite}.
"""

CLOUD_LOGGING_OR_MONITORING_DISABLED_ERROR_MSG = """\
Flag --enable-stackdriver-kubernetes requires Cloud Logging and Cloud Monitoring enabled with --enable-cloud-logging and --enable-cloud-monitoring.
"""

CLOUDRUN_STACKDRIVER_KUBERNETES_DISABLED_ERROR_MSG = """\
The CloudRun-on-GKE addon (--addons=CloudRun) requires Cloud Logging and Cloud Monitoring to be enabled via the --enable-stackdriver-kubernetes flag.
"""

CLOUDRUN_ISTIO_KUBERNETES_DISABLED_ERROR_MSG = """\
The CloudRun-on-GKE addon (--addons=CloudRun) requires Istio to be enabled via the --addons=Istio flag.
"""

CLOUDRUN_INGRESS_KUBERNETES_DISABLED_ERROR_MSG = """\
The CloudRun-on-GKE addon (--addons=CloudRun) requires HTTP Load Balancing to be enabled via the --addons=HttpLoadBalancing flag.
"""

DEFAULT_MAX_PODS_PER_NODE_WITHOUT_IP_ALIAS_ERROR_MSG = """\
Cannot use --default-max-pods-per-node without --enable-ip-alias.
"""

MAX_PODS_PER_NODE_WITHOUT_IP_ALIAS_ERROR_MSG = """\
Cannot use --max-pods-per-node without --enable-ip-alias.
"""

NOTHING_TO_UPDATE_ERROR_MSG = """\
Nothing to update.
"""

ENABLE_PRIVATE_NODES_WITH_PRIVATE_CLUSTER_ERROR_MSG = """\
Cannot specify both --[no-]enable-private-nodes and --[no-]private-cluster at the same time.
"""

ENABLE_MPI_EMPTY_ERROR_MSG = """\
Cannot use --federating-service-account without --enable-managed-pod-identity
"""

ENABLE_NETWORK_EGRESS_METERING_ERROR_MSG = """\
Cannot use --[no-]enable-network-egress-metering without --resource-usage-bigquery-dataset.
"""

ENABLE_RESOURCE_CONSUMPTION_METERING_ERROR_MSG = """\
Cannot use --[no-]enable-resource-consumption-metering without --resource-usage-bigquery-dataset.
"""

# TODO(b/132353882): move this check to the server side.
SURGE_AND_AUTOSCALING_ERROR_MSG = """\
Cannot use cluster autoscaling with alpha surge upgrades.
"""

# TODO(b/132353882): move this check to the server side.
SURGE_AND_MAX_UNAVAILABLE_BOTH_ZERO_ERROR_MSG = """\
Cannot set both --max-unavailable-upgrade and --max-surge-upgrade to 0.
"""

# TODO(b/132353882): move this check to the server side.
SURGE_REQUIRED_TO_CHANGE_MAX_UNAVAILABLE_ERROR_MSG = """\
At present changing --max-unavailable-upgrade requires setting --max-surge-upgrade to at least 1.
"""

DISABLE_DEFAULT_SNAT_WITHOUT_IP_ALIAS_ERROR_MSG = """\
Cannot use --disable-default-snat without --enable-ip-alias.
"""

DISABLE_DEFAULT_SNAT_WITHOUT_PRIVATE_NODES_ERROR_MSG = """\
Cannot use --disable-default-snat without --enable-private-nodes.
"""

MAX_NODES_PER_POOL = 1000

MAX_CONCURRENT_NODE_COUNT = 20

MAX_AUTHORIZED_NETWORKS_CIDRS = 50

INGRESS = 'HttpLoadBalancing'
HPA = 'HorizontalPodAutoscaling'
DASHBOARD = 'KubernetesDashboard'
CLOUDRUN = 'CloudRun'
CONFIGCONNECTOR = 'ConfigConnector'
ISTIO = 'Istio'
NETWORK_POLICY = 'NetworkPolicy'
NODELOCALDNS = 'NodeLocalDNS'
RESOURCE_LIMITS = 'resourceLimits'
SERVICE_ACCOUNT = 'serviceAccount'
SCOPES = 'scopes'
AUTOPROVISIONING_LOCATIONS = 'autoprovisioningLocations'
DEFAULT_ADDONS = [INGRESS, HPA]
ADDONS_OPTIONS = DEFAULT_ADDONS + [DASHBOARD, NETWORK_POLICY]
BETA_ADDONS_OPTIONS = ADDONS_OPTIONS + [ISTIO, CLOUDRUN]
ALPHA_ADDONS_OPTIONS = BETA_ADDONS_OPTIONS + [NODELOCALDNS, CONFIGCONNECTOR]


def CheckResponse(response):
  """Wrap http_wrapper.CheckResponse to skip retry on 503."""
  if response.status_code == 503:
    raise apitools_exceptions.HttpError.FromResponse(response)
  return http_wrapper.CheckResponse(response)


def NewAPIAdapter(api_version):
  if api_version == 'v1alpha1':
    return NewV1Alpha1APIAdapter()
  elif api_version == 'v1beta1':
    return NewV1Beta1APIAdapter()
  else:
    return NewV1APIAdapter()


def NewV1APIAdapter():
  return InitAPIAdapter('v1', V1Adapter)


def NewV1Beta1APIAdapter():
  return InitAPIAdapter('v1beta1', V1Beta1Adapter)


def NewV1Alpha1APIAdapter():
  return InitAPIAdapter('v1alpha1', V1Alpha1Adapter)


def InitAPIAdapter(api_version, adapter):
  """Initialize an api adapter.

  Args:
    api_version: the api version we want.
    adapter: the api adapter constructor.

  Returns:
    APIAdapter object.
  """

  api_client = core_apis.GetClientInstance('container', api_version)
  api_client.check_response_func = CheckResponse
  messages = core_apis.GetMessagesModule('container', api_version)

  registry = cloud_resources.REGISTRY.Clone()
  registry.RegisterApiByName('container', api_version)
  registry.RegisterApiByName('compute', 'v1')

  return adapter(registry, api_client, messages)


_SERVICE_ACCOUNT_SCOPES = ('https://www.googleapis.com/auth/cloud-platform',
                           'https://www.googleapis.com/auth/userinfo.email')


def NodeIdentityOptionsToNodeConfig(options, node_config):
  """Convert node identity options into node config.

  If scopes are specified with the `--scopes` flag, respect them.
  If no scopes are presented, 'gke-default' will be passed here indicating that
  we should use the default set:
  - If no service account is specified, default set is GKE_DEFAULT_SCOPES which
    is handled by ExpandScopeURIs:
    - https://www.googleapis.com/auth/devstorage.read_only,
    - https://www.googleapis.com/auth/logging.write',
    - https://www.googleapis.com/auth/monitoring,
    - https://www.googleapis.com/auth/servicecontrol,
    - https://www.googleapis.com/auth/service.management.readonly,
    - https://www.googleapis.com/auth/trace.append,
  - If a service account is specified, default set is _SERVICE_ACCOUNT_SCOPES:
    - https://www.googleapis.com/auth/cloud-platform
    - https://www.googleapis.com/auth/userinfo.email
  Args:
    options: the CreateCluster or CreateNodePool options.
    node_config: the messages.node_config object to be populated.
  """
  if options.service_account:
    node_config.serviceAccount = options.service_account
    replaced_scopes = []
    for scope in options.scopes:
      if scope == 'gke-default':
        replaced_scopes.extend(_SERVICE_ACCOUNT_SCOPES)
      else:
        replaced_scopes.append(scope)
    options.scopes = replaced_scopes

  options.scopes = ExpandScopeURIs(options.scopes)
  node_config.oauthScopes = sorted(set(options.scopes))


def ExpandScopeURIs(scopes):
  """Expand scope names to the fully qualified uris.

  Args:
    scopes: [str,] list of scope names. Can be short names ('compute-rw') or
    full urls ('https://www.googleapis.com/auth/compute'). See SCOPES in
      api_lib/container/constants.py & api_lib/compute/constants.py.

  Returns:
    list of str, full urls for recognized scopes.
  """

  scope_uris = []
  for scope in scopes:
    # Expand any scope aliases (like 'storage-rw') that the user provided
    # to their official URL representation.
    expanded = constants.SCOPES.get(scope, [scope])
    scope_uris.extend(expanded)
  return scope_uris


class CreateClusterOptions(object):
  """Options to pass to CreateCluster."""

  def __init__(self,
               node_machine_type=None,
               node_source_image=None,
               node_disk_size_gb=None,
               scopes=None,
               num_nodes=None,
               additional_zones=None,
               node_locations=None,
               user=None,
               password=None,
               cluster_version=None,
               node_version=None,
               network=None,
               cluster_ipv4_cidr=None,
               enable_cloud_logging=None,
               enable_cloud_monitoring=None,
               enable_stackdriver_kubernetes=None,
               subnetwork=None,
               addons=None,
               istio_config=None,
               local_ssd_count=None,
               local_ssd_volume_configs=None,
               node_pool_name=None,
               tags=None,
               node_labels=None,
               node_taints=None,
               enable_autoscaling=None,
               min_nodes=None,
               max_nodes=None,
               image_type=None,
               image=None,
               image_project=None,
               image_family=None,
               issue_client_certificate=None,
               max_nodes_per_pool=None,
               enable_kubernetes_alpha=None,
               preemptible=None,
               enable_autorepair=None,
               enable_autoupgrade=None,
               service_account=None,
               enable_master_authorized_networks=None,
               master_authorized_networks=None,
               enable_legacy_authorization=None,
               labels=None,
               disk_type=None,
               enable_network_policy=None,
               services_ipv4_cidr=None,
               enable_ip_alias=None,
               create_subnetwork=None,
               cluster_secondary_range_name=None,
               services_secondary_range_name=None,
               accelerators=None,
               enable_binauthz=None,
               min_cpu_platform=None,
               workload_metadata_from_node=None,
               maintenance_window=None,
               enable_pod_security_policy=None,
               allow_route_overlap=None,
               private_cluster=None,
               enable_private_nodes=None,
               enable_private_endpoint=None,
               enable_peering_route_sharing=None,
               master_ipv4_cidr=None,
               tpu_ipv4_cidr=None,
               enable_tpu=None,
               enable_tpu_service_networking=None,
               default_max_pods_per_node=None,
               max_pods_per_node=None,
               enable_managed_pod_identity=None,
               federating_service_account=None,
               resource_usage_bigquery_dataset=None,
               security_group=None,
               enable_private_ipv6_access=None,
               enable_intra_node_visibility=None,
               enable_vertical_pod_autoscaling=None,
               security_profile=None,
               security_profile_runtime_rules=None,
               database_encryption=None,
               metadata=None,
               enable_network_egress_metering=None,
               enable_resource_consumption_metering=None,
               identity_namespace=None,
               enable_shielded_nodes=None,
               linux_sysctls=None,
               autoprovisioning_service_account=None,
               autoprovisioning_scopes=None,
               disable_default_snat=None,
               autoprovisioning_locations=None,
               shielded_secure_boot=None,
               shielded_integrity_monitoring=None):
    self.node_machine_type = node_machine_type
    self.node_source_image = node_source_image
    self.node_disk_size_gb = node_disk_size_gb
    self.scopes = scopes
    self.num_nodes = num_nodes
    self.additional_zones = additional_zones
    self.node_locations = node_locations
    self.user = user
    self.password = password
    self.cluster_version = cluster_version
    self.node_version = node_version
    self.network = network
    self.cluster_ipv4_cidr = cluster_ipv4_cidr
    self.enable_cloud_logging = enable_cloud_logging
    self.enable_cloud_monitoring = enable_cloud_monitoring
    self.enable_stackdriver_kubernetes = enable_stackdriver_kubernetes
    self.subnetwork = subnetwork
    self.addons = addons
    self.istio_config = istio_config
    self.local_ssd_count = local_ssd_count
    self.local_ssd_volume_configs = local_ssd_volume_configs
    self.node_pool_name = node_pool_name
    self.tags = tags
    self.node_labels = node_labels
    self.node_taints = node_taints
    self.enable_autoscaling = enable_autoscaling
    self.min_nodes = min_nodes
    self.max_nodes = max_nodes
    self.image_type = image_type
    self.image = image
    self.image_project = image_project
    self.image_family = image_family
    self.max_nodes_per_pool = max_nodes_per_pool
    self.enable_kubernetes_alpha = enable_kubernetes_alpha
    self.preemptible = preemptible
    self.enable_autorepair = enable_autorepair
    self.enable_autoupgrade = enable_autoupgrade
    self.service_account = service_account
    self.enable_master_authorized_networks = enable_master_authorized_networks
    self.master_authorized_networks = master_authorized_networks
    self.enable_legacy_authorization = enable_legacy_authorization
    self.enable_network_policy = enable_network_policy
    self.labels = labels
    self.disk_type = disk_type
    self.services_ipv4_cidr = services_ipv4_cidr
    self.enable_ip_alias = enable_ip_alias
    self.create_subnetwork = create_subnetwork
    self.cluster_secondary_range_name = cluster_secondary_range_name
    self.services_secondary_range_name = services_secondary_range_name
    self.accelerators = accelerators
    self.enable_binauthz = enable_binauthz
    self.min_cpu_platform = min_cpu_platform
    self.workload_metadata_from_node = workload_metadata_from_node
    self.maintenance_window = maintenance_window
    self.enable_pod_security_policy = enable_pod_security_policy
    self.allow_route_overlap = allow_route_overlap
    self.private_cluster = private_cluster
    self.enable_private_nodes = enable_private_nodes
    self.enable_private_endpoint = enable_private_endpoint
    self.enable_peering_route_sharing = enable_peering_route_sharing
    self.master_ipv4_cidr = master_ipv4_cidr
    self.tpu_ipv4_cidr = tpu_ipv4_cidr
    self.enable_tpu_service_networking = enable_tpu_service_networking
    self.enable_tpu = enable_tpu
    self.issue_client_certificate = issue_client_certificate
    self.default_max_pods_per_node = default_max_pods_per_node
    self.max_pods_per_node = max_pods_per_node
    self.enable_managed_pod_identity = enable_managed_pod_identity
    self.federating_service_account = federating_service_account
    self.resource_usage_bigquery_dataset = resource_usage_bigquery_dataset
    self.security_group = security_group
    self.enable_private_ipv6_access = enable_private_ipv6_access
    self.enable_intra_node_visibility = enable_intra_node_visibility
    self.enable_vertical_pod_autoscaling = enable_vertical_pod_autoscaling
    self.security_profile = security_profile
    self.security_profile_runtime_rules = security_profile_runtime_rules
    self.database_encryption = database_encryption
    self.metadata = metadata
    self.enable_network_egress_metering = enable_network_egress_metering
    self.enable_resource_consumption_metering = enable_resource_consumption_metering
    self.identity_namespace = identity_namespace
    self.enable_shielded_nodes = enable_shielded_nodes
    self.linux_sysctls = linux_sysctls
    self.autoprovisioning_service_account = autoprovisioning_service_account
    self.autoprovisioning_scopes = autoprovisioning_scopes
    self.disable_default_snat = disable_default_snat
    self.autoprovisioning_locations = autoprovisioning_locations
    self.shielded_secure_boot = shielded_secure_boot
    self.shielded_integrity_monitoring = shielded_integrity_monitoring


class UpdateClusterOptions(object):
  """Options to pass to UpdateCluster."""

  def __init__(self,
               version=None,
               update_master=None,
               update_nodes=None,
               node_pool=None,
               monitoring_service=None,
               logging_service=None,
               enable_stackdriver_kubernetes=None,
               disable_addons=None,
               istio_config=None,
               enable_autoscaling=None,
               min_nodes=None,
               max_nodes=None,
               image_type=None,
               image=None,
               image_project=None,
               locations=None,
               enable_master_authorized_networks=None,
               master_authorized_networks=None,
               enable_autoprovisioning=None,
               enable_pod_security_policy=None,
               enable_binauthz=None,
               concurrent_node_count=None,
               enable_vertical_pod_autoscaling=None,
               enable_intra_node_visibility=None,
               security_profile=None,
               security_profile_runtime_rules=None,
               autoscaling_profile=None,
               enable_peering_route_sharing=None,
               identity_namespace=None,
               disable_workload_identity=None,
               enable_shielded_nodes=None,
               autoprovisioning_service_account=None,
               autoprovisioning_scopes=None,
               disable_default_snat=None,
               autoprovisioning_locations=None,
               resource_usage_bigquery_dataset=None,
               enable_network_egress_metering=None,
               enable_resource_consumption_metering=None,
               database_encryption=None):
    self.version = version
    self.update_master = bool(update_master)
    self.update_nodes = bool(update_nodes)
    self.node_pool = node_pool
    self.monitoring_service = monitoring_service
    self.logging_service = logging_service
    self.enable_stackdriver_kubernetes = enable_stackdriver_kubernetes
    self.disable_addons = disable_addons
    self.istio_config = istio_config
    self.enable_autoscaling = enable_autoscaling
    self.min_nodes = min_nodes
    self.max_nodes = max_nodes
    self.image_type = image_type
    self.image = image
    self.image_project = image_project
    self.locations = locations
    self.enable_master_authorized_networks = enable_master_authorized_networks
    self.master_authorized_networks = master_authorized_networks
    self.enable_autoprovisioning = enable_autoprovisioning
    self.enable_pod_security_policy = enable_pod_security_policy
    self.enable_binauthz = enable_binauthz
    self.concurrent_node_count = concurrent_node_count
    self.enable_vertical_pod_autoscaling = enable_vertical_pod_autoscaling
    self.security_profile = security_profile
    self.security_profile_runtime_rules = security_profile_runtime_rules
    self.autoscaling_profile = autoscaling_profile
    self.enable_intra_node_visibility = enable_intra_node_visibility
    self.enable_peering_route_sharing = enable_peering_route_sharing
    self.identity_namespace = identity_namespace
    self.disable_workload_identity = disable_workload_identity
    self.enable_shielded_nodes = enable_shielded_nodes
    self.autoprovisioning_service_account = autoprovisioning_service_account
    self.autoprovisioning_scopes = autoprovisioning_scopes
    self.disable_default_snat = disable_default_snat
    self.autoprovisioning_locations = autoprovisioning_locations
    self.resource_usage_bigquery_dataset = resource_usage_bigquery_dataset
    self.enable_network_egress_metering = enable_network_egress_metering
    self.enable_resource_consumption_metering = (
        enable_resource_consumption_metering)
    self.database_encryption = database_encryption


class SetMasterAuthOptions(object):
  """Options to pass to SetMasterAuth."""

  SET_PASSWORD = 'SetPassword'
  GENERATE_PASSWORD = 'GeneratePassword'
  SET_USERNAME = 'SetUsername'

  def __init__(self, action=None, username=None, password=None):
    self.action = action
    self.username = username
    self.password = password


class SetNetworkPolicyOptions(object):
  """Options to pass to SetNetworkPolicy."""

  def __init__(self, enabled):
    self.enabled = enabled


class CreateNodePoolOptions(object):
  """Options to pass to CreateNodePool."""

  def __init__(self,
               machine_type=None,
               disk_size_gb=None,
               scopes=None,
               node_version=None,
               num_nodes=None,
               local_ssd_count=None,
               local_ssd_volume_configs=None,
               tags=None,
               node_labels=None,
               node_taints=None,
               enable_autoscaling=None,
               max_nodes=None,
               min_nodes=None,
               enable_autoprovisioning=None,
               image_type=None,
               image=None,
               image_project=None,
               image_family=None,
               preemptible=None,
               enable_autorepair=None,
               enable_autoupgrade=None,
               service_account=None,
               disk_type=None,
               accelerators=None,
               min_cpu_platform=None,
               workload_metadata_from_node=None,
               max_pods_per_node=None,
               sandbox=None,
               metadata=None,
               linux_sysctls=None,
               max_surge_upgrade=None,
               max_unavailable_upgrade=None,
               node_locations=None,
               shielded_secure_boot=None,
               shielded_integrity_monitoring=None):
    self.machine_type = machine_type
    self.disk_size_gb = disk_size_gb
    self.scopes = scopes
    self.node_version = node_version
    self.num_nodes = num_nodes
    self.local_ssd_count = local_ssd_count
    self.local_ssd_volume_configs = local_ssd_volume_configs
    self.tags = tags
    self.node_labels = node_labels
    self.node_taints = node_taints
    self.enable_autoscaling = enable_autoscaling
    self.max_nodes = max_nodes
    self.min_nodes = min_nodes
    self.enable_autoprovisioning = enable_autoprovisioning
    self.image_type = image_type
    self.image = image
    self.image_project = image_project
    self.image_family = image_family
    self.preemptible = preemptible
    self.enable_autorepair = enable_autorepair
    self.enable_autoupgrade = enable_autoupgrade
    self.service_account = service_account
    self.disk_type = disk_type
    self.accelerators = accelerators
    self.min_cpu_platform = min_cpu_platform
    self.workload_metadata_from_node = workload_metadata_from_node
    self.max_pods_per_node = max_pods_per_node
    self.sandbox = sandbox
    self.metadata = metadata
    self.linux_sysctls = linux_sysctls
    self.max_surge_upgrade = max_surge_upgrade
    self.max_unavailable_upgrade = max_unavailable_upgrade
    self.node_locations = node_locations
    self.shielded_secure_boot = shielded_secure_boot
    self.shielded_integrity_monitoring = shielded_integrity_monitoring


class UpdateNodePoolOptions(object):
  """Options to pass to UpdateNodePool."""

  def __init__(self,
               enable_autorepair=None,
               enable_autoupgrade=None,
               enable_autoscaling=None,
               max_nodes=None,
               min_nodes=None,
               enable_autoprovisioning=None,
               workload_metadata_from_node=None,
               node_locations=None):
    self.enable_autorepair = enable_autorepair
    self.enable_autoupgrade = enable_autoupgrade
    self.enable_autoscaling = enable_autoscaling
    self.max_nodes = max_nodes
    self.min_nodes = min_nodes
    self.enable_autoprovisioning = enable_autoprovisioning
    self.workload_metadata_from_node = workload_metadata_from_node
    self.node_locations = node_locations

  def IsAutoscalingUpdate(self):
    return (self.enable_autoscaling is not None or self.max_nodes is not None or
            self.min_nodes is not None or
            self.enable_autoprovisioning is not None)

  def IsNodePoolManagementUpdate(self):
    return (self.enable_autorepair is not None or
            self.enable_autoupgrade is not None)

  def IsUpdateNodePoolRequest(self):
    return (self.workload_metadata_from_node is not None or
            self.node_locations is not None)


class APIAdapter(object):
  """Handles making api requests in a version-agnostic way."""

  def __init__(self, registry, client, messages):
    self.registry = registry
    self.client = client
    self.messages = messages

  def ParseCluster(self, name, location):
    # TODO(b/63383536): Migrate to container.projects.locations.clusters when
    # apiserver supports it.
    return self.registry.Parse(
        name,
        params={
            'projectId': properties.VALUES.core.project.GetOrFail,
            'zone': location,
        },
        collection='container.projects.zones.clusters')

  def ParseOperation(self, operation_id, location):
    # TODO(b/63383536): Migrate to container.projects.locations.operations when
    # apiserver supports it.
    return self.registry.Parse(
        operation_id,
        params={
            'projectId': properties.VALUES.core.project.GetOrFail,
            'zone': location,
        },
        collection='container.projects.zones.operations')

  def ParseNodePool(self, node_pool_id, location):
    # TODO(b/63383536): Migrate to container.projects.locations.nodePools when
    # apiserver supports it.
    return self.registry.Parse(
        node_pool_id,
        params={
            'projectId': properties.VALUES.core.project.GetOrFail,
            'clusterId': properties.VALUES.container.cluster.GetOrFail,
            'zone': location,
        },
        collection='container.projects.zones.clusters.nodePools')

  def GetCluster(self, cluster_ref):
    """Get a running cluster.

    Args:
      cluster_ref: cluster Resource to describe.

    Returns:
      Cluster message.
    Raises:
      Error: if cluster cannot be found or caller is missing permissions. Will
        attempt to find similar clusters in other zones for a more useful error
        if the user has list permissions.
    """
    try:
      return self.client.projects_locations_clusters.Get(
          self.messages.ContainerProjectsLocationsClustersGetRequest(
              name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref
                                          .zone, cluster_ref.clusterId)))
    except apitools_exceptions.HttpNotFoundError as error:
      api_error = exceptions.HttpException(error, util.HTTP_ERROR_FORMAT)
      # Cluster couldn't be found, maybe user got the location wrong?
      self.CheckClusterOtherZones(cluster_ref, api_error)
    except apitools_exceptions.HttpError as error:
      raise exceptions.HttpException(error, util.HTTP_ERROR_FORMAT)

  def CheckClusterOtherZones(self, cluster_ref, api_error):
    """Searches for similar cluster in other zones and reports via error.

    Args:
      cluster_ref: cluster Resource to look for others with the same id.
      api_error: current error from original request.

    Raises:
      Error: wrong zone error if another similar cluster found, otherwise not
      found error.
    """
    not_found_error = util.Error(
        NO_SUCH_CLUSTER_ERROR_MSG.format(
            error=api_error,
            name=cluster_ref.clusterId,
            project=cluster_ref.projectId))
    try:
      clusters = self.ListClusters(cluster_ref.projectId).clusters
    except apitools_exceptions.HttpForbiddenError as error:
      # Raise the default 404 Not Found error.
      # 403 Forbidden error shouldn't be raised for this unrequested list.
      raise not_found_error
    except apitools_exceptions.HttpError as error:
      raise exceptions.HttpException(error, util.HTTP_ERROR_FORMAT)
    for cluster in clusters:
      if cluster.name == cluster_ref.clusterId:
        # User likely got zone wrong.
        raise util.Error(
            WRONG_ZONE_ERROR_MSG.format(
                error=api_error,
                name=cluster_ref.clusterId,
                wrong_zone=self.Zone(cluster_ref),
                zone=cluster.zone))
    # Couldn't find a cluster with that name.
    raise not_found_error

  def FindNodePool(self, cluster, pool_name=None):
    """Find the node pool with the given name in the cluster."""
    msg = ''
    if pool_name:
      for np in cluster.nodePools:
        if np.name == pool_name:
          return np
      msg = NO_SUCH_NODE_POOL_ERROR_MSG.format(
          cluster=cluster.name, name=pool_name) + os.linesep
    elif len(cluster.nodePools) == 1:
      return cluster.nodePools[0]
    # Couldn't find a node pool with that name or a node pool was not specified.
    msg += NO_NODE_POOL_SELECTED_ERROR_MSG + os.linesep.join(
        [np.name for np in cluster.nodePools])
    raise util.Error(msg)

  def GetOperation(self, operation_ref):
    return self.client.projects_locations_operations.Get(
        self.messages.ContainerProjectsLocationsOperationsGetRequest(
            name=ProjectLocationOperation(operation_ref.projectId, operation_ref
                                          .zone, operation_ref.operationId)))

  def WaitForOperation(self,
                       operation_ref,
                       message,
                       timeout_s=1200,
                       poll_period_s=5):
    """Poll container Operation until its status is done or timeout reached.

    Args:
      operation_ref: operation resource.
      message: str, message to display to user while polling.
      timeout_s: number, seconds to poll with retries before timing out.
      poll_period_s: number, delay in seconds between requests.

    Returns:
      Operation: the return value of the last successful operations.get
      request.

    Raises:
      Error: if the operation times out or finishes with an error.
    """
    detail_message = None
    with progress_tracker.ProgressTracker(
        message, autotick=True, detail_message_callback=lambda: detail_message):
      start_time = time.time()
      while timeout_s > (time.time() - start_time):
        try:
          operation = self.GetOperation(operation_ref)
          if self.IsOperationFinished(operation):
            # Success!
            log.info('Operation %s succeeded after %.3f seconds', operation,
                     (time.time() - start_time))
            break
          detail_message = operation.detail
        except apitools_exceptions.HttpError as error:
          log.debug('GetOperation failed: %s', error)
          if error.status_code == six.moves.http_client.FORBIDDEN:
            raise exceptions.HttpException(error, util.HTTP_ERROR_FORMAT)
          # Keep trying until we timeout in case error is transient.
        time.sleep(poll_period_s)
    if not self.IsOperationFinished(operation):
      log.err.Print('Timed out waiting for operation {0}'.format(operation))
      raise util.Error('Operation [{0}] is still running'.format(operation))
    if self.GetOperationError(operation):
      raise util.Error('Operation [{0}] finished with error: {1}'.format(
          operation, self.GetOperationError(operation)))

    return operation

  def Zone(self, cluster_ref):
    # TODO(b/72146704): Remove this method.
    return cluster_ref.zone

  def CreateClusterCommon(self, cluster_ref, options):
    """Returns a CreateCluster operation."""
    node_config = self.ParseNodeConfig(options)
    pools = self.ParseNodePools(options, node_config)

    cluster = self.messages.Cluster(name=cluster_ref.clusterId, nodePools=pools)
    if options.additional_zones:
      cluster.locations = sorted([cluster_ref.zone] + options.additional_zones)
    if options.node_locations:
      cluster.locations = sorted(options.node_locations)
    if options.cluster_version:
      cluster.initialClusterVersion = options.cluster_version
    if options.network:
      cluster.network = options.network
    if options.cluster_ipv4_cidr:
      cluster.clusterIpv4Cidr = options.cluster_ipv4_cidr
    if options.enable_stackdriver_kubernetes:
      if options.enable_cloud_logging and options.enable_cloud_monitoring:
        cluster.loggingService = 'logging.googleapis.com/kubernetes'
        cluster.monitoringService = 'monitoring.googleapis.com/kubernetes'
      else:
        raise util.Error(CLOUD_LOGGING_OR_MONITORING_DISABLED_ERROR_MSG)
    if not options.enable_cloud_logging:
      cluster.loggingService = 'none'
    if not options.enable_cloud_monitoring:
      cluster.monitoringService = 'none'
    if options.subnetwork:
      cluster.subnetwork = options.subnetwork
    if options.addons:
      addons = self._AddonsConfig(
          disable_ingress=INGRESS not in options.addons,
          disable_hpa=HPA not in options.addons,
          disable_dashboard=DASHBOARD not in options.addons,
          disable_network_policy=(NETWORK_POLICY not in options.addons),
          enable_node_local_dns=(NODELOCALDNS in options.addons),
          enable_config_connector=(CONFIGCONNECTOR in options.addons),
      )
      cluster.addonsConfig = addons

    self.ParseMasterAuthorizedNetworkOptions(options, cluster)

    if options.enable_kubernetes_alpha:
      cluster.enableKubernetesAlpha = options.enable_kubernetes_alpha

    if options.default_max_pods_per_node is not None:
      if not options.enable_ip_alias:
        raise util.Error(DEFAULT_MAX_PODS_PER_NODE_WITHOUT_IP_ALIAS_ERROR_MSG)
      cluster.defaultMaxPodsConstraint = self.messages.MaxPodsConstraint(
          maxPodsPerNode=options.default_max_pods_per_node)

    if options.disable_default_snat:
      if not options.enable_ip_alias:
        raise util.Error(DISABLE_DEFAULT_SNAT_WITHOUT_IP_ALIAS_ERROR_MSG)
      if not options.enable_private_nodes:
        raise util.Error(DISABLE_DEFAULT_SNAT_WITHOUT_PRIVATE_NODES_ERROR_MSG)
      if cluster.networkConfig is None:
        cluster.networkConfig = self.messages.NetworkConfig(
            disableDefaultSnat=options.disable_default_snat)
      else:
        cluster.networkConfig.disableDefaultSnat = options.disable_default_snat

    if options.enable_legacy_authorization is not None:
      cluster.legacyAbac = self.messages.LegacyAbac(
          enabled=bool(options.enable_legacy_authorization))

    # Only Calico is currently supported as a network policy provider.
    if options.enable_network_policy:
      cluster.networkPolicy = self.messages.NetworkPolicy(
          enabled=options.enable_network_policy,
          provider=self.messages.NetworkPolicy.ProviderValueValuesEnum.CALICO)

    if options.enable_binauthz is not None:
      cluster.binaryAuthorization = self.messages.BinaryAuthorization(
          enabled=options.enable_binauthz)

    if options.maintenance_window is not None:
      policy = self.messages.MaintenancePolicy(
          window=self.messages.MaintenanceWindow(
              dailyMaintenanceWindow=self.messages.DailyMaintenanceWindow(
                  startTime=options.maintenance_window)))
      cluster.maintenancePolicy = policy

    self.ParseResourceLabels(options, cluster)

    if options.enable_pod_security_policy is not None:
      cluster.podSecurityPolicyConfig = self.messages.PodSecurityPolicyConfig(
          enabled=options.enable_pod_security_policy)

    if options.security_group is not None:
      # The presence of the --security_group="foo" flag implies enabled=True.
      cluster.authenticatorGroupsConfig = (
          self.messages.AuthenticatorGroupsConfig(
              enabled=True, securityGroup=options.security_group))
    if options.enable_shielded_nodes:
      cluster.shieldedNodes = self.messages.ShieldedNodes(enabled=True)

    self.ParseIPAliasOptions(options, cluster)
    self.ParseAllowRouteOverlapOptions(options, cluster)
    self.ParsePrivateClusterOptions(options, cluster)
    self.ParseTpuOptions(options, cluster)
    if options.enable_vertical_pod_autoscaling is not None:
      cluster.verticalPodAutoscaling = self.messages.VerticalPodAutoscaling(
          enabled=options.enable_vertical_pod_autoscaling)

    if options.resource_usage_bigquery_dataset:
      bigquery_destination = self.messages.BigQueryDestination(
          datasetId=options.resource_usage_bigquery_dataset)
      cluster.resourceUsageExportConfig = \
          self.messages.ResourceUsageExportConfig(
              bigqueryDestination=bigquery_destination)
      if options.enable_network_egress_metering:
        cluster.resourceUsageExportConfig.enableNetworkEgressMetering = True
      if options.enable_resource_consumption_metering is not None:
        cluster.resourceUsageExportConfig.consumptionMeteringConfig = \
            self.messages.ConsumptionMeteringConfig(
                enabled=options.enable_resource_consumption_metering)
    elif options.enable_network_egress_metering is not None:
      raise util.Error(ENABLE_NETWORK_EGRESS_METERING_ERROR_MSG)
    elif options.enable_resource_consumption_metering is not None:
      raise util.Error(ENABLE_RESOURCE_CONSUMPTION_METERING_ERROR_MSG)

    # Only instantiate the masterAuth struct if one or both of `user` or
    # `issue_client_certificate` is configured. Server-side Basic auth default
    # behavior is dependent on the absence of the MasterAuth struct. For this
    # reason, if only `issue_client_certificate` is configured, Basic auth will
    # be disabled.
    if options.user is not None or options.issue_client_certificate is not None:
      cluster.masterAuth = self.messages.MasterAuth(
          username=options.user, password=options.password)
      if options.issue_client_certificate is not None:
        cluster.masterAuth.clientCertificateConfig = (
            self.messages.ClientCertificateConfig(
                issueClientCertificate=options.issue_client_certificate))

    if options.enable_intra_node_visibility is not None:
      if cluster.networkConfig is None:
        cluster.networkConfig = self.messages.NetworkConfig(
            enableIntraNodeVisibility=options.enable_intra_node_visibility)
      else:
        cluster.networkConfig.enableIntraNodeVisibility = \
            options.enable_intra_node_visibility

    return cluster

  def ParseNodeConfig(self, options):
    """Creates node config based on node config options."""
    node_config = self.messages.NodeConfig()
    if options.node_machine_type:
      node_config.machineType = options.node_machine_type
    if options.node_disk_size_gb:
      node_config.diskSizeGb = options.node_disk_size_gb
    if options.disk_type:
      node_config.diskType = options.disk_type
    if options.node_source_image:
      raise util.Error('cannot specify node source image in container v1 api')

    NodeIdentityOptionsToNodeConfig(options, node_config)

    if options.local_ssd_count:
      node_config.localSsdCount = options.local_ssd_count

    if options.tags:
      node_config.tags = options.tags
    else:
      node_config.tags = []

    if options.image_type:
      node_config.imageType = options.image_type

    self.ParseCustomNodeConfig(options, node_config)

    _AddNodeLabelsToNodeConfig(node_config, options)
    _AddMetadataToNodeConfig(node_config, options)
    self._AddNodeTaintsToNodeConfig(node_config, options)

    if options.preemptible:
      node_config.preemptible = options.preemptible

    self.ParseAcceleratorOptions(options, node_config)

    if options.min_cpu_platform is not None:
      node_config.minCpuPlatform = options.min_cpu_platform

    _AddWorkloadMetadataToNodeConfig(node_config, options, self.messages)
    _AddLinuxNodeConfigToNodeConfig(node_config, options, self.messages)
    _AddShieldedInstanceConfigToNodeConfig(node_config, options, self.messages)

    return node_config

  def ParseCustomNodeConfig(self, options, node_config):
    """Parses custom node config options."""
    custom_config = self.messages.CustomImageConfig()
    if options.image:
      custom_config.image = options.image
    if options.image_project:
      custom_config.imageProject = options.image_project
    if options.image_family:
      custom_config.imageFamily = options.image_family
    if options.image or options.image_project or options.image_family:
      node_config.nodeImageConfig = custom_config

  def ParseNodePools(self, options, node_config):
    """Creates a list of node pools for the cluster by parsing options.

    Args:
      options: cluster creation options
      node_config: node configuration for nodes in the node pools

    Returns:
      List of node pools.
    """
    max_nodes_per_pool = options.max_nodes_per_pool or MAX_NODES_PER_POOL
    pools = (options.num_nodes + max_nodes_per_pool - 1) // max_nodes_per_pool
    if pools == 1:
      pool_names = ['default-pool']  # pool consistency with server default
    else:
      # default-pool-0, -1, ...
      pool_names = ['default-pool-{0}'.format(i) for i in range(0, pools)]

    pools = []
    per_pool = (options.num_nodes + len(pool_names) - 1) // len(pool_names)
    to_add = options.num_nodes
    for name in pool_names:
      nodes = per_pool if (to_add > per_pool) else to_add
      pool = self.messages.NodePool(
          name=name,
          initialNodeCount=nodes,
          config=node_config,
          version=options.node_version,
          management=self._GetNodeManagement(options))
      if options.enable_autoscaling:
        pool.autoscaling = self.messages.NodePoolAutoscaling(
            enabled=options.enable_autoscaling,
            minNodeCount=options.min_nodes,
            maxNodeCount=options.max_nodes)
      if options.max_pods_per_node:
        if not options.enable_ip_alias:
          raise util.Error(MAX_PODS_PER_NODE_WITHOUT_IP_ALIAS_ERROR_MSG)
        pool.maxPodsConstraint = self.messages.MaxPodsConstraint(
            maxPodsPerNode=options.max_pods_per_node)
      pools.append(pool)
      to_add -= nodes
    return pools

  def ParseAcceleratorOptions(self, options, node_config):
    """Parses accrelerator options for the nodes in the cluster."""
    if options.accelerators is not None:
      type_name = options.accelerators['type']
      # Accelerator count defaults to 1.
      count = int(options.accelerators.get('count', 1))
      node_config.accelerators = [
          self.messages.AcceleratorConfig(
              acceleratorType=type_name, acceleratorCount=count)
      ]

  def ParseResourceLabels(self, options, cluster):
    """Parses resource labels options for the cluster."""
    if options.labels is not None:
      labels = self.messages.Cluster.ResourceLabelsValue()
      props = []
      for k, v in sorted(six.iteritems(options.labels)):
        props.append(labels.AdditionalProperty(key=k, value=v))
      labels.additionalProperties = props
      cluster.resourceLabels = labels

  def ParseIPAliasOptions(self, options, cluster):
    """Parses the options for IP Alias."""
    ip_alias_only_options = [
        ('services-ipv4-cidr', options.services_ipv4_cidr),
        ('create-subnetwork', options.create_subnetwork),
        ('cluster-secondary-range-name', options.cluster_secondary_range_name),
        ('services-secondary-range-name', options.services_secondary_range_name)
    ]
    if not options.enable_ip_alias:
      for name, opt in ip_alias_only_options:
        if opt:
          raise util.Error(
              PREREQUISITE_OPTION_ERROR_MSG.format(
                  prerequisite='enable-ip-alias', opt=name))

    if options.subnetwork and options.create_subnetwork is not None:
      raise util.Error(CREATE_SUBNETWORK_WITH_SUBNETWORK_ERROR_MSG)

    if options.enable_ip_alias:
      subnetwork_name = None
      node_ipv4_cidr = None

      if options.create_subnetwork is not None:
        for key in options.create_subnetwork:
          if key not in ['name', 'range']:
            raise util.Error(
                CREATE_SUBNETWORK_INVALID_KEY_ERROR_MSG.format(key=key))
        subnetwork_name = options.create_subnetwork.get('name', None)
        node_ipv4_cidr = options.create_subnetwork.get('range', None)

      policy = self.messages.IPAllocationPolicy(
          useIpAliases=options.enable_ip_alias,
          createSubnetwork=options.create_subnetwork is not None,
          subnetworkName=subnetwork_name,
          clusterIpv4CidrBlock=options.cluster_ipv4_cidr,
          nodeIpv4CidrBlock=node_ipv4_cidr,
          servicesIpv4CidrBlock=options.services_ipv4_cidr,
          clusterSecondaryRangeName=options.cluster_secondary_range_name,
          servicesSecondaryRangeName=options.services_secondary_range_name)
      if options.tpu_ipv4_cidr:
        policy.tpuIpv4CidrBlock = options.tpu_ipv4_cidr
      if options.enable_tpu_service_networking:
        policy.tpuUseServiceNetworking = options.enable_tpu_service_networking
      cluster.clusterIpv4Cidr = None
      cluster.ipAllocationPolicy = policy
    else:
      cluster.ipAllocationPolicy = self.messages.IPAllocationPolicy(
          useIpAliases=options.enable_ip_alias)
    return cluster

  def ParseAllowRouteOverlapOptions(self, options, cluster):
    """Parse the options for allow route overlap."""
    if not options.allow_route_overlap:
      return
    # Validate required flags are set.
    if options.cluster_ipv4_cidr is None:
      raise util.Error(ALLOW_ROUTE_OVERLAP_WITHOUT_CLUSTER_CIDR_ERROR_MSG)
    if options.enable_ip_alias and options.services_ipv4_cidr is None:
      raise util.Error(ALLOW_ROUTE_OVERLAP_WITHOUT_SERVICES_CIDR_ERROR_MSG)

    # Fill in corresponding field.
    if cluster.ipAllocationPolicy is None:
      policy = self.messages.IPAllocationPolicy(allowRouteOverlap=True)
      cluster.ipAllocationPolicy = policy
    else:
      cluster.ipAllocationPolicy.allowRouteOverlap = True

  def ParsePrivateClusterOptions(self, options, cluster):
    """Parses the options for Private Clusters."""
    if (options.enable_private_nodes is not None and
        options.private_cluster is not None):
      raise util.Error(ENABLE_PRIVATE_NODES_WITH_PRIVATE_CLUSTER_ERROR_MSG)

    if options.enable_private_nodes is None:
      options.enable_private_nodes = options.private_cluster

    if options.enable_private_nodes and not options.enable_ip_alias:
      raise util.Error(
          PREREQUISITE_OPTION_ERROR_MSG.format(
              prerequisite='enable-ip-alias', opt='enable-private-nodes'))

    if options.enable_private_endpoint and not options.enable_private_nodes:
      raise util.Error(
          PREREQUISITE_OPTION_ERROR_MSG.format(
              prerequisite='enable-private-nodes',
              opt='enable-private-endpoint'))

    if options.master_ipv4_cidr and not options.enable_private_nodes:
      raise util.Error(
          PREREQUISITE_OPTION_ERROR_MSG.format(
              prerequisite='enable-private-nodes', opt='master-ipv4-cidr'))

    if options.enable_private_nodes:
      config = self.messages.PrivateClusterConfig(
          enablePrivateNodes=options.enable_private_nodes,
          enablePrivateEndpoint=options.enable_private_endpoint,
          masterIpv4CidrBlock=options.master_ipv4_cidr)
      cluster.privateClusterConfig = config
    return cluster

  def ParseTpuOptions(self, options, cluster):
    """Parses the options for TPUs."""
    if options.enable_tpu and not options.enable_ip_alias:
      # Raises error if use --enable-tpu without --enable-ip-alias.
      raise util.Error(
          PREREQUISITE_OPTION_ERROR_MSG.format(
              prerequisite='enable-ip-alias', opt='enable-tpu'))

    if not options.enable_tpu and options.tpu_ipv4_cidr:
      # Raises error if use --tpu-ipv4-cidr without --enable-tpu.
      raise util.Error(
          PREREQUISITE_OPTION_ERROR_MSG.format(
              prerequisite='enable-tpu', opt='tpu-ipv4-cidr'))

    if not options.enable_tpu and options.enable_tpu_service_networking:
      # Raises error if use --enable-tpu-service-networking without
      # --enable-tpu.
      raise util.Error(
          PREREQUISITE_OPTION_ERROR_MSG.format(
              prerequisite='enable-tpu', opt='enable-tpu-service-networking'))

    if options.enable_tpu:
      cluster.enableTpu = options.enable_tpu

  def ParseMasterAuthorizedNetworkOptions(self, options, cluster):
    """Parses the options for master authorized networks."""
    if (options.master_authorized_networks and
        not options.enable_master_authorized_networks):
      # Raise error if use --master-authorized-networks without
      # --enable-master-authorized-networks.
      raise util.Error(MISMATCH_AUTHORIZED_NETWORKS_ERROR_MSG)
    elif options.enable_master_authorized_networks is None:
      cluster.masterAuthorizedNetworksConfig = None
    elif not options.enable_master_authorized_networks:
      authorized_networks = self.messages.MasterAuthorizedNetworksConfig(
          enabled=False)
      cluster.masterAuthorizedNetworksConfig = authorized_networks
    else:
      authorized_networks = self.messages.MasterAuthorizedNetworksConfig(
          enabled=options.enable_master_authorized_networks)
      if options.master_authorized_networks:
        for network in options.master_authorized_networks:
          authorized_networks.cidrBlocks.append(
              self.messages.CidrBlock(cidrBlock=network))
      cluster.masterAuthorizedNetworksConfig = authorized_networks

  def CreateCluster(self, cluster_ref, options):
    cluster = self.CreateClusterCommon(cluster_ref, options)
    req = self.messages.CreateClusterRequest(
        parent=ProjectLocation(cluster_ref.projectId, cluster_ref.zone),
        cluster=cluster)
    operation = self.client.projects_locations_clusters.Create(req)
    return self.ParseOperation(operation.name, cluster_ref.zone)

  def CreateClusterAutoscalingCommon(self, *_):
    raise util.Error(NO_AUTOPROVISIONING_MSG)

  def UpdateClusterCommon(self, cluster_ref, options):
    """Returns an UpdateCluster operation."""

    update = None
    if not options.version:
      options.version = '-'
    if options.update_nodes:
      update = self.messages.ClusterUpdate(
          desiredNodeVersion=options.version,
          desiredNodePoolId=options.node_pool,
          desiredImageType=options.image_type,
          desiredImage=options.image,
          desiredImageProject=options.image_project)
      # security_profile may be set in upgrade command
      if options.security_profile is not None:
        update.securityProfile = self.messages.SecurityProfile(
            name=options.security_profile)
    elif options.update_master:
      update = self.messages.ClusterUpdate(desiredMasterVersion=options.version)
      # security_profile may be set in upgrade command
      if options.security_profile is not None:
        update.securityProfile = self.messages.SecurityProfile(
            name=options.security_profile)
    elif options.enable_stackdriver_kubernetes:
      update = self.messages.ClusterUpdate()
      update.desiredLoggingService = 'logging.googleapis.com/kubernetes'
      update.desiredMonitoringService = 'monitoring.googleapis.com/kubernetes'
    elif options.monitoring_service or options.logging_service:
      update = self.messages.ClusterUpdate()
      if options.monitoring_service:
        update.desiredMonitoringService = options.monitoring_service
      if options.logging_service:
        update.desiredLoggingService = options.logging_service
    elif options.disable_addons:
      addons = self._AddonsConfig(
          disable_ingress=options.disable_addons.get(INGRESS),
          disable_hpa=options.disable_addons.get(HPA),
          disable_dashboard=options.disable_addons.get(DASHBOARD),
          disable_network_policy=options.disable_addons.get(NETWORK_POLICY))
      update = self.messages.ClusterUpdate(desiredAddonsConfig=addons)
    elif options.enable_autoscaling is not None:
      # For update, we can either enable or disable.
      autoscaling = self.messages.NodePoolAutoscaling(
          enabled=options.enable_autoscaling)
      if options.enable_autoscaling:
        autoscaling.minNodeCount = options.min_nodes
        autoscaling.maxNodeCount = options.max_nodes
      update = self.messages.ClusterUpdate(
          desiredNodePoolId=options.node_pool,
          desiredNodePoolAutoscaling=autoscaling)
    elif options.locations:
      update = self.messages.ClusterUpdate(desiredLocations=options.locations)
    elif options.enable_master_authorized_networks is not None:
      # For update, we can either enable or disable.
      authorized_networks = self.messages.MasterAuthorizedNetworksConfig(
          enabled=options.enable_master_authorized_networks)
      if options.master_authorized_networks:
        for network in options.master_authorized_networks:
          authorized_networks.cidrBlocks.append(
              self.messages.CidrBlock(cidrBlock=network))
      update = self.messages.ClusterUpdate(
          desiredMasterAuthorizedNetworksConfig=authorized_networks)
    elif options.enable_autoprovisioning is not None or \
         options.autoscaling_profile is not None:
      autoscaling = self.CreateClusterAutoscalingCommon(cluster_ref, options)
      update = self.messages.ClusterUpdate(
          desiredClusterAutoscaling=autoscaling)
    elif options.enable_pod_security_policy is not None:
      config = self.messages.PodSecurityPolicyConfig(
          enabled=options.enable_pod_security_policy)
      update = self.messages.ClusterUpdate(
          desiredPodSecurityPolicyConfig=config)
    elif options.enable_binauthz is not None:
      binary_authorization = self.messages.BinaryAuthorization(
          enabled=options.enable_binauthz)
      update = self.messages.ClusterUpdate(
          desiredBinaryAuthorization=binary_authorization)
    elif options.enable_vertical_pod_autoscaling is not None:
      vertical_pod_autoscaling = self.messages.VerticalPodAutoscaling(
          enabled=options.enable_vertical_pod_autoscaling)
      update = self.messages.ClusterUpdate(
          desiredVerticalPodAutoscaling=vertical_pod_autoscaling)
    elif options.resource_usage_bigquery_dataset is not None:
      export_config = self.messages.ResourceUsageExportConfig(
          bigqueryDestination=self.messages.BigQueryDestination(
              datasetId=options.resource_usage_bigquery_dataset))
      if options.enable_network_egress_metering:
        export_config.enableNetworkEgressMetering = True
      if options.enable_resource_consumption_metering is not None:
        export_config.consumptionMeteringConfig = \
            self.messages.ConsumptionMeteringConfig(
                enabled=options.enable_resource_consumption_metering)
      update = self.messages.ClusterUpdate(
          desiredResourceUsageExportConfig=export_config)
    elif options.enable_network_egress_metering is not None:
      raise util.Error(ENABLE_NETWORK_EGRESS_METERING_ERROR_MSG)
    elif options.enable_resource_consumption_metering is not None:
      raise util.Error(ENABLE_RESOURCE_CONSUMPTION_METERING_ERROR_MSG)
    elif options.clear_resource_usage_bigquery_dataset is not None:
      export_config = self.messages.ResourceUsageExportConfig()
      update = self.messages.ClusterUpdate(
          desiredResourceUsageExportConfig=export_config)
    elif options.security_profile is not None:
      # security_profile is set in update command
      security_profile = self.messages.SecurityProfile(
          name=options.security_profile)
      update = self.messages.ClusterUpdate(securityProfile=security_profile)
    elif options.enable_intra_node_visibility is not None:
      intra_node_visibility_config = self.messages.IntraNodeVisibilityConfig(
          enabled=options.enable_intra_node_visibility)
      update = self.messages.ClusterUpdate(
          desiredIntraNodeVisibilityConfig=intra_node_visibility_config)
    elif options.enable_peering_route_sharing is not None:
      # For update, we can either enable or disable.
      private_cluster_config = self.messages.PrivateClusterConfig(
          enablePeeringRouteSharing=options.enable_peering_route_sharing)
      update = self.messages.ClusterUpdate(
          desiredPrivateClusterConfig=private_cluster_config)

    if (options.security_profile is not None and
        options.security_profile_runtime_rules is not None):
      update.securityProfile.disableRuntimeRules = \
          not options.security_profile_runtime_rules
    if (options.master_authorized_networks and
        not options.enable_master_authorized_networks):
      # Raise error if use --master-authorized-networks without
      # --enable-master-authorized-networks.
      raise util.Error(MISMATCH_AUTHORIZED_NETWORKS_ERROR_MSG)

    return update

  def UpdateCluster(self, cluster_ref, options):
    """Handles UpdateCluster options that are specific to a release track.

    Overridden in each release track.

    Args:
      cluster_ref: Name and location of the cluster.
      options: An UpdateClusterOptions containining the user-specified options.

    Returns:
      The operation to be executed.
    """

    update = self.UpdateClusterCommon(cluster_ref, options)

    if not update:
      # if reached here, it's possible:
      # - someone added update flags but not handled
      # - none of the update flags specified from command line
      # so raise an error with readable message like:
      #   Nothing to update
      # to catch this error.
      raise util.Error(NOTHING_TO_UPDATE_ERROR_MSG)

    op = self.client.projects_locations_clusters.Update(
        self.messages.UpdateClusterRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId),
            update=update))
    return self.ParseOperation(op.name, cluster_ref.zone)

  def SetLoggingService(self, cluster_ref, logging_service):
    op = self.client.projects_locations_clusters.SetLogging(
        self.messages.SetLoggingServiceRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId),
            loggingService=logging_service))
    return self.ParseOperation(op.name, cluster_ref.zone)

  def SetLegacyAuthorization(self, cluster_ref, enable_legacy_authorization):
    op = self.client.projects_locations_clusters.SetLegacyAbac(
        self.messages.SetLegacyAbacRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId),
            enabled=bool(enable_legacy_authorization)))
    return self.ParseOperation(op.name, cluster_ref.zone)

  def _AddonsConfig(self,
                    disable_ingress=None,
                    disable_hpa=None,
                    disable_dashboard=None,
                    disable_network_policy=None,
                    enable_node_local_dns=None,
                    enable_config_connector=None):
    """Generates an AddonsConfig object given specific parameters.

    Args:
      disable_ingress: whether to disable the GCLB ingress controller.
      disable_hpa: whether to disable the horizontal pod autoscaling controller.
      disable_dashboard: whether to disable the Kuberntes Dashboard.
      disable_network_policy: whether to disable NetworkPolicy enforcement.
      enable_node_local_dns: whether to enable NodeLocalDNS cache.
      enable_config_connector: whether to enable ConfigConnector.

    Returns:
      An AddonsConfig object that contains the options defining what addons to
      run in the cluster.
    """
    addons = self.messages.AddonsConfig()
    if disable_ingress is not None:
      addons.httpLoadBalancing = self.messages.HttpLoadBalancing(
          disabled=disable_ingress)
    if disable_hpa is not None:
      addons.horizontalPodAutoscaling = self.messages.HorizontalPodAutoscaling(
          disabled=disable_hpa)
    if disable_dashboard is not None:
      addons.kubernetesDashboard = self.messages.KubernetesDashboard(
          disabled=disable_dashboard)
    # Network policy is disabled by default.
    if disable_network_policy is not None:
      addons.networkPolicyConfig = self.messages.NetworkPolicyConfig(
          disabled=disable_network_policy)
    if enable_node_local_dns:
      addons.dnsCacheConfig = self.messages.DnsCacheConfig(enabled=True)
    if enable_config_connector:
      addons.configConnectorConfig = self.messages.ConfigConnectorConfig(
          enabled=True)
    return addons

  def _AddLocalSSDVolumeConfigsToNodeConfig(self, node_config, options):
    """Add LocalSSDVolumeConfigs to nodeConfig."""
    if options.local_ssd_volume_configs is None:
      return
    format_enum = self.messages.LocalSsdVolumeConfig.FormatValueValuesEnum
    local_ssd_volume_configs_list = []
    for config in options.local_ssd_volume_configs:
      count = int(config['count'])
      ssd_type = config['type'].lower()
      if config['format'].lower() == 'fs':
        ssd_format = format_enum.FS
      elif config['format'].lower() == 'block':
        ssd_format = format_enum.BLOCK
      else:
        raise util.Error(
            LOCAL_SSD_INCORRECT_FORMAT_ERROR_MSG.format(
                err_format=config['format']))
      local_ssd_volume_configs_list.append(
          self.messages.LocalSsdVolumeConfig(
              count=count, type=ssd_type, format=ssd_format))
    node_config.localSsdVolumeConfigs = local_ssd_volume_configs_list

  def _AddNodeTaintsToNodeConfig(self, node_config, options):
    """Add nodeTaints to nodeConfig."""
    if options.node_taints is None:
      return
    taints = []
    effect_enum = self.messages.NodeTaint.EffectValueValuesEnum
    for key, value in sorted(six.iteritems(options.node_taints)):
      strs = value.split(':')
      if len(strs) != 2:
        raise util.Error(
            NODE_TAINT_INCORRECT_FORMAT_ERROR_MSG.format(key=key, value=value))
      value = strs[0]
      taint_effect = strs[1]
      if taint_effect == 'NoSchedule':
        effect = effect_enum.NO_SCHEDULE
      elif taint_effect == 'PreferNoSchedule':
        effect = effect_enum.PREFER_NO_SCHEDULE
      elif taint_effect == 'NoExecute':
        effect = effect_enum.NO_EXECUTE
      else:
        raise util.Error(
            NODE_TAINT_INCORRECT_EFFECT_ERROR_MSG.format(effect=strs[1]))
      taints.append(
          self.messages.NodeTaint(key=key, value=value, effect=effect))

    node_config.taints = taints

  def SetNetworkPolicyCommon(self, options):
    """Returns a SetNetworkPolicy operation."""
    return self.messages.NetworkPolicy(
        enabled=options.enabled,
        # Only Calico is currently supported as a network policy provider.
        provider=self.messages.NetworkPolicy.ProviderValueValuesEnum.CALICO)

  def SetNetworkPolicy(self, cluster_ref, options):
    netpol = self.SetNetworkPolicyCommon(options)
    req = self.messages.SetNetworkPolicyRequest(
        name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                    cluster_ref.clusterId),
        networkPolicy=netpol)
    return self.ParseOperation(
        self.client.projects_locations_clusters.SetNetworkPolicy(req).name,
        cluster_ref.zone)

  def SetMasterAuthCommon(self, options):
    """Returns a SetMasterAuth action."""
    update = self.messages.MasterAuth(
        username=options.username, password=options.password)
    if options.action == SetMasterAuthOptions.SET_PASSWORD:
      action = (
          self.messages.SetMasterAuthRequest.ActionValueValuesEnum.SET_PASSWORD)
    elif options.action == SetMasterAuthOptions.GENERATE_PASSWORD:
      action = (
          self.messages.SetMasterAuthRequest.ActionValueValuesEnum
          .GENERATE_PASSWORD)
    else:  # options.action == SetMasterAuthOptions.SET_USERNAME
      action = (
          self.messages.SetMasterAuthRequest.ActionValueValuesEnum.SET_USERNAME)
    return update, action

  def SetMasterAuth(self, cluster_ref, options):
    update, action = self.SetMasterAuthCommon(options)
    req = self.messages.SetMasterAuthRequest(
        name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                    cluster_ref.clusterId),
        action=action,
        update=update)
    op = self.client.projects_locations_clusters.SetMasterAuth(req)
    return self.ParseOperation(op.name, cluster_ref.zone)

  def StartIpRotation(self, cluster_ref, rotate_credentials):
    operation = self.client.projects_locations_clusters.StartIpRotation(
        self.messages.StartIPRotationRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId),
            rotateCredentials=rotate_credentials))
    return self.ParseOperation(operation.name, cluster_ref.zone)

  def CompleteIpRotation(self, cluster_ref):
    operation = self.client.projects_locations_clusters.CompleteIpRotation(
        self.messages.CompleteIPRotationRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId)))
    return self.ParseOperation(operation.name, cluster_ref.zone)

  def SetMaintenanceWindow(self, cluster_ref, maintenance_window):
    """Updates maintenance window for a cluster."""
    policy = self.messages.MaintenancePolicy(
        window=self.messages.MaintenanceWindow(
            dailyMaintenanceWindow=self.messages.DailyMaintenanceWindow(
                startTime=maintenance_window)))
    req = self.messages.SetMaintenancePolicyRequest(
        name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                    cluster_ref.clusterId),
        maintenancePolicy=policy)
    if maintenance_window == 'None':
      req.maintenancePolicy = None

    operation = self.client.projects_locations_clusters.SetMaintenancePolicy(
        req)

    return self.ParseOperation(operation.name, cluster_ref.zone)

  def DeleteCluster(self, cluster_ref):
    """Delete a running cluster.

    Args:
      cluster_ref: cluster Resource to describe

    Returns:
      Cluster message.
    Raises:
      Error: if cluster cannot be found or caller is missing permissions. Will
        attempt to find similar clusters in other zones for a more useful error
        if the user has list permissions.
    """
    try:
      operation = self.client.projects_locations_clusters.Delete(
          self.messages.ContainerProjectsLocationsClustersDeleteRequest(
              name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref
                                          .zone, cluster_ref.clusterId)))
      return self.ParseOperation(operation.name, cluster_ref.zone)
    except apitools_exceptions.HttpNotFoundError as error:
      api_error = exceptions.HttpException(error, util.HTTP_ERROR_FORMAT)
      # Cluster couldn't be found, maybe user got the location wrong?
      self.CheckClusterOtherZones(cluster_ref, api_error)

  def ListClusters(self, project, location=None):
    if not location:
      location = '-'
    req = self.messages.ContainerProjectsLocationsClustersListRequest(
        parent=ProjectLocation(project, location))
    return self.client.projects_locations_clusters.List(req)

  def CreateNodePoolCommon(self, node_pool_ref, options):
    """Returns a CreateNodePool operation."""
    node_config = self.messages.NodeConfig()
    if options.machine_type:
      node_config.machineType = options.machine_type
    if options.disk_size_gb:
      node_config.diskSizeGb = options.disk_size_gb
    if options.disk_type:
      node_config.diskType = options.disk_type
    if options.image_type:
      node_config.imageType = options.image_type

    custom_config = self.messages.CustomImageConfig()
    if options.image:
      custom_config.image = options.image
    if options.image_project:
      custom_config.imageProject = options.image_project
    if options.image_family:
      custom_config.imageFamily = options.image_family
    if options.image or options.image_project or options.image_family:
      node_config.nodeImageConfig = custom_config

    NodeIdentityOptionsToNodeConfig(options, node_config)

    if options.local_ssd_count:
      node_config.localSsdCount = options.local_ssd_count
    if options.local_ssd_volume_configs:
      self._AddLocalSSDVolumeConfigsToNodeConfig(node_config, options)
    if options.tags:
      node_config.tags = options.tags
    else:
      node_config.tags = []

    if options.accelerators is not None:
      type_name = options.accelerators['type']
      # Accelerator count defaults to 1.
      count = int(options.accelerators.get('count', 1))
      node_config.accelerators = [
          self.messages.AcceleratorConfig(
              acceleratorType=type_name, acceleratorCount=count)
      ]

    _AddMetadataToNodeConfig(node_config, options)
    _AddNodeLabelsToNodeConfig(node_config, options)
    self._AddNodeTaintsToNodeConfig(node_config, options)

    if options.preemptible:
      node_config.preemptible = options.preemptible

    if options.min_cpu_platform is not None:
      node_config.minCpuPlatform = options.min_cpu_platform

    _AddWorkloadMetadataToNodeConfig(node_config, options, self.messages)
    _AddLinuxNodeConfigToNodeConfig(node_config, options, self.messages)
    _AddShieldedInstanceConfigToNodeConfig(node_config, options, self.messages)

    if options.sandbox is not None:
      node_config.sandboxConfig = self.messages.SandboxConfig(
          sandboxType=options.sandbox['type'])

    pool = self.messages.NodePool(
        name=node_pool_ref.nodePoolId,
        initialNodeCount=options.num_nodes,
        config=node_config,
        version=options.node_version,
        management=self._GetNodeManagement(options))

    if options.enable_autoscaling:
      pool.autoscaling = self.messages.NodePoolAutoscaling(
          enabled=options.enable_autoscaling,
          minNodeCount=options.min_nodes,
          maxNodeCount=options.max_nodes)

    if options.max_pods_per_node is not None:
      pool.maxPodsConstraint = self.messages.MaxPodsConstraint(
          maxPodsPerNode=options.max_pods_per_node)

    # When both flags are default, don't write UpgradeSettings.
    # TODO(b/130107095): remove this condition and always create
    # UppgradeSettings.
    if ((options.max_surge_upgrade is not None) and
        (options.max_surge_upgrade != 0)) or (
            (options.max_unavailable_upgrade is not None) and
            (options.max_unavailable_upgrade != 1)):
      # TODO(b/130103224): remove this condition.
      if options.enable_autoscaling:
        raise util.Error(SURGE_AND_AUTOSCALING_ERROR_MSG)
      if (options.max_surge_upgrade == 0 and
          options.max_unavailable_upgrade == 0):
        raise util.Error(SURGE_AND_MAX_UNAVAILABLE_BOTH_ZERO_ERROR_MSG)
      # TODO(b/130107094): remove this condition.
      if options.max_surge_upgrade == 0:
        raise util.Error(SURGE_REQUIRED_TO_CHANGE_MAX_UNAVAILABLE_ERROR_MSG)
      pool.upgradeSettings = self.messages.UpgradeSettings()
      pool.upgradeSettings.maxSurge = options.max_surge_upgrade
      pool.upgradeSettings.maxUnavailable = options.max_unavailable_upgrade

    if options.node_locations is not None:
      pool.locations = sorted(options.node_locations)
    return pool

  def CreateNodePool(self, node_pool_ref, options):
    """CreateNodePool creates a node pool and returns the operation."""
    pool = self.CreateNodePoolCommon(node_pool_ref, options)

    req = self.messages.CreateNodePoolRequest(
        nodePool=pool,
        parent=ProjectLocationCluster(node_pool_ref.projectId,
                                      node_pool_ref.zone,
                                      node_pool_ref.clusterId))
    operation = self.client.projects_locations_clusters_nodePools.Create(req)
    return self.ParseOperation(operation.name, node_pool_ref.zone)

  def ListNodePools(self, cluster_ref):
    req = self.messages.ContainerProjectsLocationsClustersNodePoolsListRequest(
        parent=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                      cluster_ref.clusterId))
    return self.client.projects_locations_clusters_nodePools.List(req)

  def GetNodePool(self, node_pool_ref):
    req = self.messages.ContainerProjectsLocationsClustersNodePoolsGetRequest(
        name=ProjectLocationClusterNodePool(
            node_pool_ref.projectId, node_pool_ref.zone,
            node_pool_ref.clusterId, node_pool_ref.nodePoolId))
    return self.client.projects_locations_clusters_nodePools.Get(req)

  def UpdateNodePoolNodeManagement(self, node_pool_ref, options):
    """Updates node pool's node management configuration.

    Args:
      node_pool_ref: node pool Resource to update.
      options: node pool update options

    Returns:
      Updated node management configuration.
    """
    pool = self.GetNodePool(node_pool_ref)
    node_management = pool.management
    if node_management is None:
      node_management = self.messages.NodeManagement()
    if options.enable_autorepair is not None:
      node_management.autoRepair = options.enable_autorepair
    if options.enable_autoupgrade is not None:
      node_management.autoUpgrade = options.enable_autoupgrade
    return node_management

  def UpdateNodePoolAutoscaling(self, node_pool_ref, options):
    """Update node pool's autoscaling configuration.

    Args:
      node_pool_ref: node pool Resource to update.
      options: node pool update options

    Returns:
      Updated autoscaling configuration for the node pool.
    """
    pool = self.GetNodePool(node_pool_ref)
    autoscaling = pool.autoscaling
    if autoscaling is None:
      autoscaling = self.messages.NodePoolAutoscaling()
    if options.enable_autoscaling is not None:
      autoscaling.enabled = options.enable_autoscaling
      if not autoscaling.enabled:
        # clear limits and autoprovisioned when disabling autoscaling
        autoscaling.minNodeCount = 0
        autoscaling.maxNodeCount = 0
        autoscaling.autoprovisioned = False
    if options.enable_autoprovisioning is not None:
      autoscaling.autoprovisioned = options.enable_autoprovisioning
      if autoscaling.autoprovisioned:
        # clear min nodes limit when enabling autoprovisioning
        autoscaling.minNodeCount = 0
    if options.max_nodes is not None:
      autoscaling.maxNodeCount = options.max_nodes
    if options.min_nodes is not None:
      autoscaling.minNodeCount = options.min_nodes
    return autoscaling

  def UpdateNodePool(self, node_pool_ref, options):
    """Updates nodePool on a cluster."""
    node_management = self.UpdateNodePoolNodeManagement(node_pool_ref, options)
    req = (
        self.messages.SetNodePoolManagementRequest(
            name=ProjectLocationClusterNodePool(node_pool_ref.projectId,
                                                node_pool_ref.zone,
                                                node_pool_ref.clusterId,
                                                node_pool_ref.nodePoolId),
            management=node_management))
    operation = (
        self.client.projects_locations_clusters_nodePools.SetManagement(req))
    return self.ParseOperation(operation.name, node_pool_ref.zone)

  def DeleteNodePool(self, node_pool_ref):
    operation = self.client.projects_locations_clusters_nodePools.Delete(
        self.messages.ContainerProjectsLocationsClustersNodePoolsDeleteRequest(
            name=ProjectLocationClusterNodePool(
                node_pool_ref.projectId, node_pool_ref.zone,
                node_pool_ref.clusterId, node_pool_ref.nodePoolId)))
    return self.ParseOperation(operation.name, node_pool_ref.zone)

  def RollbackUpgrade(self, node_pool_ref):
    operation = self.client.projects_locations_clusters_nodePools.Rollback(
        self.messages.RollbackNodePoolUpgradeRequest(
            name=ProjectLocationClusterNodePool(
                node_pool_ref.projectId, node_pool_ref.zone,
                node_pool_ref.clusterId, node_pool_ref.nodePoolId)))
    return self.ParseOperation(operation.name, node_pool_ref.zone)

  def CancelOperation(self, op_ref):
    req = self.messages.CancelOperationRequest(
        name=ProjectLocationOperation(op_ref.projectId, op_ref.zone,
                                      op_ref.operationId))
    return self.client.projects_locations_operations.Cancel(req)

  def IsRunning(self, cluster):
    return (
        cluster.status == self.messages.Cluster.StatusValueValuesEnum.RUNNING)

  def IsDegraded(self, cluster):
    return (
        cluster.status == self.messages.Cluster.StatusValueValuesEnum.DEGRADED)

  def GetDegradedWarning(self, cluster):
    if cluster.conditions:
      codes = [condition.code for condition in cluster.conditions]
      messages = [condition.message for condition in cluster.conditions]
      return ('Codes: {0}\n' 'Messages: {1}.').format(codes, messages)
    else:
      return gke_constants.DEFAULT_DEGRADED_WARNING

  def GetOperationError(self, operation):
    return operation.statusMessage

  def ListOperations(self, project, location=None):
    if not location:
      location = '-'
    req = self.messages.ContainerProjectsLocationsOperationsListRequest(
        parent=ProjectLocation(project, location))
    return self.client.projects_locations_operations.List(req)

  def IsOperationFinished(self, operation):
    return (
        operation.status == self.messages.Operation.StatusValueValuesEnum.DONE)

  def GetServerConfig(self, project, location):
    req = self.messages.ContainerProjectsLocationsGetServerConfigRequest(
        name=ProjectLocation(project, location))
    return self.client.projects_locations.GetServerConfig(req)

  def ResizeNodePool(self, cluster_ref, pool_name, size):
    req = self.messages.SetNodePoolSizeRequest(
        name=ProjectLocationClusterNodePool(cluster_ref.projectId,
                                            cluster_ref.zone,
                                            cluster_ref.clusterId, pool_name),
        nodeCount=size)
    operation = self.client.projects_locations_clusters_nodePools.SetSize(req)
    return self.ParseOperation(operation.name, cluster_ref.zone)

  def _GetNodeManagement(self, options):
    """Gets a wrapper containing the options for how nodes are managed.

    Args:
      options: node management options

    Returns:
      A NodeManagement object that contains the options indicating how nodes
      are managed. This is currently quite simple, containing only two options.
      However, there are more options planned for node management.
    """
    if options.enable_autorepair is None and options.enable_autoupgrade is None:
      return None

    node_management = self.messages.NodeManagement()
    node_management.autoRepair = options.enable_autorepair
    node_management.autoUpgrade = options.enable_autoupgrade
    return node_management

  def UpdateLabelsCommon(self, cluster_ref, update_labels):
    """Update labels on a cluster.

    Args:
      cluster_ref: cluster to update.
      update_labels: labels to set.

    Returns:
      Operation ref for label set operation.
    """
    clus = None
    try:
      clus = self.GetCluster(cluster_ref)
    except apitools_exceptions.HttpNotFoundError:
      pass
    except apitools_exceptions.HttpError as error:
      raise exceptions.HttpException(error, util.HTTP_ERROR_FORMAT)

    labels = self.messages.SetLabelsRequest.ResourceLabelsValue()
    props = []
    for k, v in sorted(six.iteritems(update_labels)):
      props.append(labels.AdditionalProperty(key=k, value=v))
    labels.additionalProperties = props
    return labels, clus.labelFingerprint

  def UpdateLabels(self, cluster_ref, update_labels):
    """Updates labels for a cluster."""
    labels, fingerprint = self.UpdateLabelsCommon(cluster_ref, update_labels)
    operation = self.client.projects_locations_clusters.SetResourceLabels(
        self.messages.SetLabelsRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId),
            resourceLabels=labels,
            labelFingerprint=fingerprint))
    return self.ParseOperation(operation.name, cluster_ref.zone)

  def RemoveLabelsCommon(self, cluster_ref, remove_labels):
    """Removes labels from a cluster.

    Args:
      cluster_ref: cluster to update.
      remove_labels: labels to remove.

    Returns:
      Operation ref for label set operation.
    """
    clus = None
    try:
      clus = self.GetCluster(cluster_ref)
    except apitools_exceptions.HttpNotFoundError:
      pass
    except apitools_exceptions.HttpError as error:
      raise exceptions.HttpException(error, util.HTTP_ERROR_FORMAT)

    clus_labels = {}
    if clus.resourceLabels:
      for item in clus.resourceLabels.additionalProperties:
        clus_labels[item.key] = str(item.value)

    # if clusLabels empty, nothing to do
    if not clus_labels:
      raise util.Error(NO_LABELS_ON_CLUSTER_ERROR_MSG.format(cluster=clus.name))

    for k in remove_labels:
      try:
        clus_labels.pop(k)
      except KeyError as error:
        # if at least one label not found on cluster, raise error
        raise util.Error(
            NO_SUCH_LABEL_ERROR_MSG.format(cluster=clus.name, name=k))

    labels = self.messages.SetLabelsRequest.ResourceLabelsValue()
    for k, v in sorted(six.iteritems(clus_labels)):
      labels.additionalProperties.append(
          labels.AdditionalProperty(key=k, value=v))
    return labels, clus.labelFingerprint

  def RemoveLabels(self, cluster_ref, remove_labels):
    """Removes labels from a cluster."""
    labels, fingerprint = self.RemoveLabelsCommon(cluster_ref, remove_labels)
    operation = self.client.projects_locations_clusters.SetResourceLabels(
        self.messages.SetLabelsRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId),
            resourceLabels=labels,
            labelFingerprint=fingerprint))
    return self.ParseOperation(operation.name, cluster_ref.zone)

  def GetIamPolicy(self, cluster_ref):
    raise NotImplementedError('GetIamPolicy is not overridden')

  def SetIamPolicy(self, cluster_ref):
    raise NotImplementedError('GetIamPolicy is not overridden')

  def ListUsableSubnets(self, project_ref, network_project, filter_arg):
    """List usable subnets for a given project.

    Args:
      project_ref: project where clusters will be created.
      network_project: project ID where clusters will be created.
      filter_arg: value of filter flag.

    Returns:
      Response containing the list of subnetworks and a next page token.
    """
    filters = []
    if network_project is not None:
      filters.append('networkProjectId=' + network_project)

    if filter_arg is not None:
      filters.append(filter_arg)

    filters = ' AND '.join(filters)

    req = self.messages.ContainerProjectsAggregatedUsableSubnetworksListRequest(
        # parent example: 'projects/abc'
        parent=project_ref.RelativeName(),
        # max pageSize accepted by GKE
        pageSize=500,
        filter=filters)
    return self.client.projects_aggregated_usableSubnetworks.List(req)


class V1Adapter(APIAdapter):
  """APIAdapter for v1."""


class V1Beta1Adapter(V1Adapter):
  """APIAdapter for v1beta1."""

  def CreateCluster(self, cluster_ref, options):
    cluster = self.CreateClusterCommon(cluster_ref, options)
    if options.addons:
      # CloudRun is disabled by default.
      if CLOUDRUN in options.addons:
        if not options.enable_stackdriver_kubernetes:
          raise util.Error(CLOUDRUN_STACKDRIVER_KUBERNETES_DISABLED_ERROR_MSG)
        if ISTIO not in options.addons:
          raise util.Error(CLOUDRUN_ISTIO_KUBERNETES_DISABLED_ERROR_MSG)
        if INGRESS not in options.addons:
          raise util.Error(CLOUDRUN_INGRESS_KUBERNETES_DISABLED_ERROR_MSG)
        cluster.addonsConfig.cloudRunConfig = self.messages.CloudRunConfig(
            disabled=False)
      # Istio is disabled by default
      if ISTIO in options.addons:
        istio_auth = self.messages.IstioConfig.AuthValueValuesEnum.AUTH_NONE
        mtls = self.messages.IstioConfig.AuthValueValuesEnum.AUTH_MUTUAL_TLS
        istio_config = options.istio_config
        if istio_config is not None:
          auth_config = istio_config.get('auth')
          if auth_config is not None:
            if auth_config == 'MTLS_STRICT':
              istio_auth = mtls
        cluster.addonsConfig.istioConfig = self.messages.IstioConfig(
            disabled=False, auth=istio_auth)
    if options.enable_autoprovisioning is not None:
      cluster.autoscaling = self.CreateClusterAutoscalingCommon(
          cluster_ref, options)
    if options.database_encryption:
      cluster.databaseEncryption = self.messages.DatabaseEncryption(
          keyName=options.database_encryption,
          state=self.messages.DatabaseEncryption.StateValueValuesEnum.ENCRYPTED)
    if options.identity_namespace is not None:
      cluster.workloadIdentityConfig = self.messages.WorkloadIdentityConfig(
          identityNamespace=options.identity_namespace)
    req = self.messages.CreateClusterRequest(
        parent=ProjectLocation(cluster_ref.projectId, cluster_ref.zone),
        cluster=cluster)
    operation = self.client.projects_locations_clusters.Create(req)
    return self.ParseOperation(operation.name, cluster_ref.zone)

  def UpdateCluster(self, cluster_ref, options):
    update = self.UpdateClusterCommon(cluster_ref, options)

    if options.identity_namespace:
      update = self.messages.ClusterUpdate(
          desiredWorkloadIdentityConfig=self.messages.WorkloadIdentityConfig(
              identityNamespace=options.identity_namespace))
    elif options.disable_workload_identity:
      update = self.messages.ClusterUpdate(
          desiredWorkloadIdentityConfig=self.messages.WorkloadIdentityConfig(
              identityNamespace=''))
    elif options.database_encryption:
      update = self.messages.ClusterUpdate(
          desiredDatabaseEncryption=self.messages.DatabaseEncryption(
              keyName=options.database_encryption,
              state=self.messages.DatabaseEncryption.StateValueValuesEnum
              .ENCRYPTED))

    if options.enable_shielded_nodes is not None:
      update = self.messages.ClusterUpdate(
          desiredShieldedNodes=self.messages.ShieldedNodes(
              enabled=options.enable_shielded_nodes))

    if not update:
      # if reached here, it's possible:
      # - someone added update flags but not handled
      # - none of the update flags specified from command line
      # so raise an error with readable message like:
      #   Nothing to update
      # to catch this error.
      raise util.Error(NOTHING_TO_UPDATE_ERROR_MSG)

    if options.disable_addons is not None:
      if options.disable_addons.get(ISTIO) is not None:
        istio_auth = self.messages.IstioConfig.AuthValueValuesEnum.AUTH_NONE
        mtls = self.messages.IstioConfig.AuthValueValuesEnum.AUTH_MUTUAL_TLS
        istio_config = options.istio_config
        if istio_config is not None:
          auth_config = istio_config.get('auth')
          if auth_config is not None:
            if auth_config == 'MTLS_STRICT':
              istio_auth = mtls
        update.desiredAddonsConfig.istioConfig = self.messages.IstioConfig(
            disabled=options.disable_addons.get(ISTIO), auth=istio_auth)
      if options.disable_addons.get(CLOUDRUN) is not None:
        update.desiredAddonsConfig.cloudRunConfig = (
            self.messages.CloudRunConfig(
                disabled=options.disable_addons.get(CLOUDRUN)))

    op = self.client.projects_locations_clusters.Update(
        self.messages.UpdateClusterRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId),
            update=update))
    return self.ParseOperation(op.name, cluster_ref.zone)

  def CreateClusterAutoscalingCommon(self, _cluster_ref, options):
    """Create cluster's autoscaling configuration.

    Args:
      _cluster_ref: Cluster reference.
      options: Either CreateClusterOptions or UpdateClusterOptions.

    Returns:
      Cluster's autoscaling configuration.
    """
    autoscaling = self.messages.ClusterAutoscaling()
    autoscaling.enableNodeAutoprovisioning = options.enable_autoprovisioning

    resource_limits = []
    if options.autoprovisioning_config_file is not None:
      # Create using config file only.
      autoprovisioning_options = yaml.load(options.autoprovisioning_config_file)
      resource_limits = autoprovisioning_options.get(RESOURCE_LIMITS)
      service_account = autoprovisioning_options.get(SERVICE_ACCOUNT)
      scopes = autoprovisioning_options.get(SCOPES)
      autoprovisioning_locations = \
          autoprovisioning_options.get(AUTOPROVISIONING_LOCATIONS)
    else:
      resource_limits = self.ResourceLimitsFromFlags(options)
      service_account = options.autoprovisioning_service_account
      scopes = options.autoprovisioning_scopes
      autoprovisioning_locations = options.autoprovisioning_locations

    if options.enable_autoprovisioning is not None:
      autoscaling.enableNodeAutoprovisioning = options.enable_autoprovisioning
      if resource_limits is None:
        resource_limits = []
      autoscaling.resourceLimits = resource_limits
      if scopes is None:
        scopes = []
      autoscaling.autoprovisioningNodePoolDefaults = self.messages \
        .AutoprovisioningNodePoolDefaults(serviceAccount=service_account,
                                          oauthScopes=scopes)
      if autoprovisioning_locations:
        autoscaling.autoprovisioningLocations = \
            sorted(autoprovisioning_locations)

    self.ValidateClusterAutoscaling(autoscaling)
    return autoscaling

  def ValidateClusterAutoscaling(self, autoscaling):
    """Validate cluster autoscaling configuration.

    Args:
      autoscaling: autoscaling configuration to be validated.

    Raises:
      Error if the new configuration is invalid.
    """
    if autoscaling.enableNodeAutoprovisioning:
      cpu_found, mem_found = False, False
      for limit in autoscaling.resourceLimits:
        if limit.resourceType == 'cpu':
          cpu_found = True
        if limit.resourceType == 'memory':
          mem_found = True
      if not cpu_found or not mem_found:
        raise util.Error(NO_AUTOPROVISIONING_LIMITS_ERROR_MSG)
      if autoscaling.autoprovisioningNodePoolDefaults \
        and autoscaling.autoprovisioningNodePoolDefaults.serviceAccount \
        and autoscaling.autoprovisioningNodePoolDefaults.oauthScopes:
        raise util.Error(ATMOST_ONE_IDENTITY_FOR_AUTOPROVISIONING_ERROR_MSG)
    elif autoscaling.resourceLimits:
      raise util.Error(LIMITS_WITHOUT_AUTOPROVISIONING_MSG)
    elif autoscaling.autoprovisioningNodePoolDefaults and \
        (autoscaling.autoprovisioningNodePoolDefaults.serviceAccount or
         autoscaling.autoprovisioningNodePoolDefaults.oauthScopes):
      raise util.Error(IDENTITY_DEFAULTS_WITHOUT_AUTOPROVISIONING_MSG)

  def ResourceLimitsFromFlags(self, options):
    """Create cluster's autoscaling resource limits from command line flags.

    Args:
      options: Either CreateClusterOptions or UpdateClusterOptions.

    Returns:
      Cluster's new autoscaling resource limits.
    """
    new_resource_limits = []
    if options.min_cpu is not None or options.max_cpu is not None:
      new_resource_limits.append(
          self.messages.ResourceLimit(
              resourceType='cpu',
              minimum=options.min_cpu,
              maximum=options.max_cpu))
    if options.min_memory is not None or options.max_memory is not None:
      new_resource_limits.append(
          self.messages.ResourceLimit(
              resourceType='memory',
              minimum=options.min_memory,
              maximum=options.max_memory))
    if options.max_accelerator is not None:
      accelerator_type = options.max_accelerator.get('type')
      min_count = 0
      if options.min_accelerator is not None:
        if options.min_accelerator.get('type') != accelerator_type:
          raise util.Error(MISMATCH_ACCELERATOR_TYPE_LIMITS_ERROR_MSG)
        min_count = options.min_accelerator.get('count', 0)
      new_resource_limits.append(
          self.messages.ResourceLimit(
              resourceType=options.max_accelerator.get('type'),
              minimum=min_count,
              maximum=options.max_accelerator.get('count', 0)))
    return new_resource_limits

  def UpdateNodePoolRequest(self, node_pool_ref, options):
    """Creates an UpdateNodePoolRequest from the provided options.

    Arguments:
      node_pool_ref: The node pool to act on.
      options: UpdateNodePoolOptions with the user-specified options.

    Returns:

      An UpdateNodePoolRequest.
    """

    update_request = self.messages.UpdateNodePoolRequest(
        name=ProjectLocationClusterNodePool(
            node_pool_ref.projectId,
            node_pool_ref.zone,
            node_pool_ref.clusterId,
            node_pool_ref.nodePoolId,
        ))

    if options.workload_metadata_from_node is not None:
      _AddWorkloadMetadataToNodeConfig(update_request, options, self.messages)
    elif options.node_locations is not None:
      update_request.locations = sorted(options.node_locations)

    return update_request

  def UpdateNodePool(self, node_pool_ref, options):
    if options.IsAutoscalingUpdate():
      autoscaling = self.UpdateNodePoolAutoscaling(node_pool_ref, options)
      update = self.messages.ClusterUpdate(
          desiredNodePoolId=node_pool_ref.nodePoolId,
          desiredNodePoolAutoscaling=autoscaling)
      operation = self.client.projects_locations_clusters.Update(
          self.messages.UpdateClusterRequest(
              name=ProjectLocationCluster(node_pool_ref.projectId,
                                          node_pool_ref.zone,
                                          node_pool_ref.clusterId),
              update=update))
      return self.ParseOperation(operation.name, node_pool_ref.zone)
    elif options.IsNodePoolManagementUpdate():
      management = self.UpdateNodePoolNodeManagement(node_pool_ref, options)
      req = (
          self.messages.SetNodePoolManagementRequest(
              name=ProjectLocationClusterNodePool(node_pool_ref.projectId,
                                                  node_pool_ref.zone,
                                                  node_pool_ref.clusterId,
                                                  node_pool_ref.nodePoolId),
              management=management))
      operation = (
          self.client.projects_locations_clusters_nodePools.SetManagement(req))
    elif options.IsUpdateNodePoolRequest():
      req = self.UpdateNodePoolRequest(node_pool_ref, options)
      operation = self.client.projects_locations_clusters_nodePools.Update(req)
    else:
      raise util.Error('Unhandled node pool update mode')

    return self.ParseOperation(operation.name, node_pool_ref.zone)


class V1Alpha1Adapter(V1Beta1Adapter):
  """APIAdapter for v1alpha1."""

  def CreateCluster(self, cluster_ref, options):
    cluster = self.CreateClusterCommon(cluster_ref, options)
    if (options.enable_autoprovisioning is not None or
        options.autoscaling_profile is not None):
      cluster.autoscaling = self.CreateClusterAutoscalingCommon(None, options)
    if options.local_ssd_volume_configs:
      for pool in cluster.nodePools:
        self._AddLocalSSDVolumeConfigsToNodeConfig(pool.config, options)
    if options.addons:
      # CloudRun is disabled by default.
      if CLOUDRUN in options.addons:
        if not options.enable_stackdriver_kubernetes:
          raise util.Error(CLOUDRUN_STACKDRIVER_KUBERNETES_DISABLED_ERROR_MSG)
        if ISTIO not in options.addons:
          raise util.Error(CLOUDRUN_ISTIO_KUBERNETES_DISABLED_ERROR_MSG)
        if INGRESS not in options.addons:
          raise util.Error(CLOUDRUN_INGRESS_KUBERNETES_DISABLED_ERROR_MSG)
        cluster.addonsConfig.cloudRunConfig = self.messages.CloudRunConfig(
            disabled=False)
      # Istio is disabled by default
      if ISTIO in options.addons:
        istio_auth = self.messages.IstioConfig.AuthValueValuesEnum.AUTH_NONE
        mtls = self.messages.IstioConfig.AuthValueValuesEnum.AUTH_MUTUAL_TLS
        istio_config = options.istio_config
        if istio_config is not None:
          auth_config = istio_config.get('auth')
          if auth_config is not None:
            if auth_config == 'MTLS_STRICT':
              istio_auth = mtls
        cluster.addonsConfig.istioConfig = self.messages.IstioConfig(
            disabled=False, auth=istio_auth)
    if options.enable_managed_pod_identity:
      cluster.managedPodIdentityConfig = self.messages.ManagedPodIdentityConfig(
          enabled=options.enable_managed_pod_identity,
          federatingServiceAccount=options.federating_service_account)
    elif options.federating_service_account is not None:
      raise util.Error(ENABLE_MPI_EMPTY_ERROR_MSG)
    if options.identity_namespace is not None:
      cluster.workloadIdentityConfig = self.messages.WorkloadIdentityConfig(
          identityNamespace=options.identity_namespace)
    if options.security_profile is not None:
      cluster.securityProfile = self.messages.SecurityProfile(
          name=options.security_profile)
      if options.security_profile_runtime_rules is not None:
        cluster.securityProfile.disableRuntimeRules = \
          not options.security_profile_runtime_rules
    if options.database_encryption:
      cluster.databaseEncryption = self.messages.DatabaseEncryption(
          keyName=options.database_encryption,
          state=self.messages.DatabaseEncryption.StateValueValuesEnum.ENCRYPTED)
    if options.enable_private_ipv6_access is not None:
      if cluster.networkConfig is None:
        cluster.networkConfig = self.messages.NetworkConfig(
            enablePrivateIpv6Access=options.enable_private_ipv6_access)
      else:
        cluster.networkConfig.enablePrivateIpv6Access = \
            options.enable_private_ipv6_access
    if options.enable_peering_route_sharing is not None:
      if not options.enable_private_nodes:
        raise util.Error(
            PREREQUISITE_OPTION_ERROR_MSG.format(
                prerequisite='enable-private-nodes',
                opt='enable-peering-route-sharing'))
      cluster.privateClusterConfig.enablePeeringRouteSharing = \
        options.enable_peering_route_sharing
    if options.release_channel is not None:
      channels = {
          'rapid': self.messages.ReleaseChannel.ChannelValueValuesEnum.RAPID,
      }
      cluster.releaseChannel = self.messages.ReleaseChannel(
          channel=channels[options.release_channel])

    req = self.messages.CreateClusterRequest(
        parent=ProjectLocation(cluster_ref.projectId, cluster_ref.zone),
        cluster=cluster)
    operation = self.client.projects_locations_clusters.Create(req)
    return self.ParseOperation(operation.name, cluster_ref.zone)

  def UpdateCluster(self, cluster_ref, options):
    update = self.UpdateClusterCommon(cluster_ref, options)

    if options.identity_namespace:
      update = self.messages.ClusterUpdate(
          desiredWorkloadIdentityConfig=self.messages.WorkloadIdentityConfig(
              identityNamespace=options.identity_namespace))
    elif options.disable_workload_identity:
      update = self.messages.ClusterUpdate(
          desiredWorkloadIdentityConfig=self.messages.WorkloadIdentityConfig(
              identityNamespace=''))
    elif options.database_encryption:
      update = self.messages.ClusterUpdate(
          desiredDatabaseEncryption=self.messages.DatabaseEncryption(
              keyName=options.database_encryption,
              state=self.messages.DatabaseEncryption.StateValueValuesEnum
              .ENCRYPTED))

    if options.enable_shielded_nodes is not None:
      update = self.messages.ClusterUpdate(
          desiredShieldedNodes=self.messages.ShieldedNodes(
              enabled=options.enable_shielded_nodes))

    if options.disable_default_snat is not None:
      disable_default_snat = self.messages.DefaultSnatStatus(
          disabled=options.disable_default_snat)
      update = self.messages.ClusterUpdate(
          desiredDefaultSnatStatus=disable_default_snat)

    if not update:
      # if reached here, it's possible:
      # - someone added update flags but not handled
      # - none of the update flags specified from command line
      # so raise an error with readable message like:
      #   Nothing to update
      # to catch this error.
      raise util.Error(NOTHING_TO_UPDATE_ERROR_MSG)

    if options.disable_addons is not None:
      if options.disable_addons.get(ISTIO) is not None:
        istio_auth = self.messages.IstioConfig.AuthValueValuesEnum.AUTH_NONE
        mtls = self.messages.IstioConfig.AuthValueValuesEnum.AUTH_MUTUAL_TLS
        istio_config = options.istio_config
        if istio_config is not None:
          auth_config = istio_config.get('auth')
          if auth_config is not None:
            if auth_config == 'MTLS_STRICT':
              istio_auth = mtls
        update.desiredAddonsConfig.istioConfig = self.messages.IstioConfig(
            disabled=options.disable_addons.get(ISTIO), auth=istio_auth)
      if options.disable_addons.get(CLOUDRUN) is not None:
        update.desiredAddonsConfig.cloudRunConfig = (
            self.messages.CloudRunConfig(
                disabled=options.disable_addons.get(CLOUDRUN)))
    if options.update_nodes and options.concurrent_node_count:
      update.concurrentNodeCount = options.concurrent_node_count

    op = self.client.projects_locations_clusters.Update(
        self.messages.UpdateClusterRequest(
            name=ProjectLocationCluster(cluster_ref.projectId, cluster_ref.zone,
                                        cluster_ref.clusterId),
            update=update))
    return self.ParseOperation(op.name, cluster_ref.zone)

  def CreateNodePool(self, node_pool_ref, options):
    pool = self.CreateNodePoolCommon(node_pool_ref, options)
    if options.local_ssd_volume_configs:
      self._AddLocalSSDVolumeConfigsToNodeConfig(pool.config, options)
    if options.enable_autoprovisioning is not None:
      pool.autoscaling.autoprovisioned = options.enable_autoprovisioning
    if options.node_group is not None:
      pool.config.nodeGroup = options.node_group
    req = self.messages.CreateNodePoolRequest(
        nodePool=pool,
        parent=ProjectLocationCluster(node_pool_ref.projectId,
                                      node_pool_ref.zone,
                                      node_pool_ref.clusterId))
    operation = self.client.projects_locations_clusters_nodePools.Create(req)
    return self.ParseOperation(operation.name, node_pool_ref.zone)

  def CreateClusterAutoscalingCommon(self, cluster_ref, options):
    """Create cluster's autoscaling configuration.

    Args:
      cluster_ref: Cluster reference.
      options: Either CreateClusterOptions or UpdateClusterOptions.

    Returns:
      Cluster's autoscaling configuration.
    """
    # Patch cluster autoscaling if cluster_ref is provided.
    cluster = None
    autoscaling = self.messages.ClusterAutoscaling()
    if cluster_ref:
      cluster = self.GetCluster(cluster_ref)
    if cluster and cluster.autoscaling:
      autoscaling.enableNodeAutoprovisioning = \
          cluster.autoscaling.enableNodeAutoprovisioning
      # TODO(b/131216098): Remove when passing resourceLimits and
      # autoprovisioning locations are not overridden by empty list.
      autoscaling.resourceLimits = cluster.autoscaling.resourceLimits
      autoscaling.autoprovisioningLocations = \
          cluster.autoscaling.autoprovisioningLocations

    resource_limits = []
    if options.autoprovisioning_config_file is not None:
      # Create using config file only.
      autoprovisioning_options = yaml.load(options.autoprovisioning_config_file)
      resource_limits = autoprovisioning_options.get(RESOURCE_LIMITS)
      service_account = autoprovisioning_options.get(SERVICE_ACCOUNT)
      scopes = autoprovisioning_options.get(SCOPES)
      autoprovisioning_locations = \
          autoprovisioning_options.get(AUTOPROVISIONING_LOCATIONS)
    else:
      resource_limits = self.ResourceLimitsFromFlags(options)
      service_account = options.autoprovisioning_service_account
      scopes = options.autoprovisioning_scopes
      autoprovisioning_locations = options.autoprovisioning_locations

    if options.enable_autoprovisioning is not None:
      autoscaling.enableNodeAutoprovisioning = options.enable_autoprovisioning
      if resource_limits is None:
        resource_limits = []
      autoscaling.resourceLimits = resource_limits
      if scopes is None:
        scopes = []
      autoscaling.autoprovisioningNodePoolDefaults = self.messages \
        .AutoprovisioningNodePoolDefaults(
            serviceAccount=service_account,
            oauthScopes=scopes)
      if autoprovisioning_locations:
        autoscaling.autoprovisioningLocations = \
            sorted(autoprovisioning_locations)

    if options.autoscaling_profile is not None:
      autoscaling.autoscalingProfile = \
          self.CreateAutoscalingProfileCommon(options)

    self.ValidateClusterAutoscaling(autoscaling)
    return autoscaling

  def CreateAutoscalingProfileCommon(self, options):
    if options.autoscaling_profile == 'optimize-utilization':
      return self.messages.ClusterAutoscaling \
          .AutoscalingProfileValueValuesEnum.OPTIMIZE_UTILIZATION
    if options.autoscaling_profile == 'balanced':
      return self.messages.ClusterAutoscaling \
          .AutoscalingProfileValueValuesEnum.PROFILE_UNSPECIFIED
    raise util.Error(UNKNOWN_AUTOSCALING_PROFILE_MSG \
                       .format(profile=options.autoscaling_profile))

  def ParseNodePools(self, options, node_config):
    """Creates a list of node pools for the cluster by parsing options.

    Args:
      options: cluster creation options
      node_config: node configuration for nodes in the node pools

    Returns:
      List of node pools.
    """
    max_nodes_per_pool = options.max_nodes_per_pool or MAX_NODES_PER_POOL
    num_pools = (options.num_nodes + max_nodes_per_pool -
                 1) // max_nodes_per_pool
    # pool consistency with server default
    node_pool_name = options.node_pool_name or 'default-pool'

    if num_pools == 1:
      pool_names = [node_pool_name]
    else:
      # default-pool-0, -1, ... or some-pool-0, -1 where some-pool is user
      # supplied
      pool_names = [
          '{0}-{1}'.format(node_pool_name, i) for i in range(0, num_pools)
      ]

    pools = []
    nodes_per_pool = (options.num_nodes + num_pools - 1) // len(pool_names)
    to_add = options.num_nodes
    for name in pool_names:
      nodes = nodes_per_pool if (to_add > nodes_per_pool) else to_add
      pool = self.messages.NodePool(
          name=name,
          initialNodeCount=nodes,
          config=node_config,
          version=options.node_version,
          management=self._GetNodeManagement(options))
      if options.enable_autoscaling:
        pool.autoscaling = self.messages.NodePoolAutoscaling(
            enabled=options.enable_autoscaling,
            minNodeCount=options.min_nodes,
            maxNodeCount=options.max_nodes)
      if options.max_pods_per_node:
        if not options.enable_ip_alias:
          raise util.Error(MAX_PODS_PER_NODE_WITHOUT_IP_ALIAS_ERROR_MSG)
        pool.maxPodsConstraint = self.messages.MaxPodsConstraint(
            maxPodsPerNode=options.max_pods_per_node)
      # When both flags are default, don't write UpgradeSettings.
      # TODO(b/130107095): remove this condition and always create
      # UppgradeSettings.
      if (options.max_surge_upgrade != 0 or
          options.max_unavailable_upgrade != 1):
        # TODO(b/130103224): remove this condition.
        if options.enable_autoscaling:
          raise util.Error(SURGE_AND_AUTOSCALING_ERROR_MSG)
        if (options.max_surge_upgrade == 0 and
            options.max_unavailable_upgrade == 0):
          raise util.Error(SURGE_AND_MAX_UNAVAILABLE_BOTH_ZERO_ERROR_MSG)
        # TODO(b/130107094): remove this condition.
        if options.max_surge_upgrade == 0:
          raise util.Error(SURGE_REQUIRED_TO_CHANGE_MAX_UNAVAILABLE_ERROR_MSG)
        pool.upgradeSettings = self.messages.UpgradeSettings()
        pool.upgradeSettings.maxSurge = options.max_surge_upgrade
        pool.upgradeSettings.maxUnavailable = options.max_unavailable_upgrade
      pools.append(pool)
      to_add -= nodes
    return pools

  def GetIamPolicy(self, cluster_ref):
    return self.client.projects.GetIamPolicy(
        self.messages.ContainerProjectsGetIamPolicyRequest(
            resource=ProjectLocationCluster(cluster_ref.projectId, cluster_ref
                                            .zone, cluster_ref.clusterId)))

  def SetIamPolicy(self, cluster_ref, policy):
    return self.client.projects.SetIamPolicy(
        self.messages.ContainerProjectsSetIamPolicyRequest(
            googleIamV1SetIamPolicyRequest=self.messages
            .GoogleIamV1SetIamPolicyRequest(policy=policy),
            resource=ProjectLocationCluster(cluster_ref.projectId,
                                            cluster_ref.zone,
                                            cluster_ref.clusterId)))


def _AddMetadataToNodeConfig(node_config, options):
  if not options.metadata:
    return
  metadata = node_config.MetadataValue()
  props = []
  for key, value in six.iteritems(options.metadata):
    props.append(metadata.AdditionalProperty(key=key, value=value))
  metadata.additionalProperties = props
  node_config.metadata = metadata


def _AddNodeLabelsToNodeConfig(node_config, options):
  if options.node_labels is None:
    return
  labels = node_config.LabelsValue()
  props = []
  for key, value in six.iteritems(options.node_labels):
    props.append(labels.AdditionalProperty(key=key, value=value))
  labels.additionalProperties = props
  node_config.labels = labels


def _AddWorkloadMetadataToNodeConfig(node_config, options, messages):
  """Adds workload metadata to NodeConfig."""
  if options.workload_metadata_from_node is not None:
    option = options.workload_metadata_from_node
    if option == 'UNSPECIFIED':
      node_config.workloadMetadataConfig = messages.WorkloadMetadataConfig(
          nodeMetadata=messages.WorkloadMetadataConfig
          .NodeMetadataValueValuesEnum.UNSPECIFIED)
    elif option == 'SECURE':
      node_config.workloadMetadataConfig = messages.WorkloadMetadataConfig(
          nodeMetadata=messages.WorkloadMetadataConfig
          .NodeMetadataValueValuesEnum.SECURE)
    elif option == 'EXPOSED':
      node_config.workloadMetadataConfig = messages.WorkloadMetadataConfig(
          nodeMetadata=messages.WorkloadMetadataConfig
          .NodeMetadataValueValuesEnum.EXPOSE)
    elif option == 'GKE_METADATA_SERVER':
      node_config.workloadMetadataConfig = messages.WorkloadMetadataConfig(
          nodeMetadata=messages.WorkloadMetadataConfig
          .NodeMetadataValueValuesEnum.GKE_METADATA_SERVER)
    else:
      raise util.Error(
          UNKNOWN_WORKLOAD_METADATA_FROM_NODE_ERROR_MSG.format(option=option))


def _AddLinuxNodeConfigToNodeConfig(node_config, options, messages):
  """Adds LinuxNodeConfig to NodeConfig."""

  # Linux kernel parameters (sysctls).
  if options.linux_sysctls:
    if not node_config.linuxNodeConfig:
      node_config.linuxNodeConfig = messages.LinuxNodeConfig()
    linux_sysctls = node_config.linuxNodeConfig.SysctlsValue()
    props = []
    for key, value in six.iteritems(options.linux_sysctls):
      props.append(linux_sysctls.AdditionalProperty(key=key, value=value))
    linux_sysctls.additionalProperties = props

    node_config.linuxNodeConfig.sysctls = linux_sysctls


def _AddShieldedInstanceConfigToNodeConfig(node_config, options, messages):
  """Adds ShieldedInstanceConfig to NodeConfig."""
  if (options.shielded_secure_boot is not None or
      options.shielded_integrity_monitoring is not None):
    node_config.shieldedInstanceConfig = messages.ShieldedInstanceConfig()
    if options.shielded_secure_boot is not None:
      node_config.shieldedInstanceConfig.enableSecureBoot = (
          options.shielded_secure_boot)
    if options.shielded_integrity_monitoring is not None:
      node_config.shieldedInstanceConfig.enableIntegrityMonitoring = (
          options.shielded_integrity_monitoring)


def ProjectLocation(project, location):
  return 'projects/' + project + '/locations/' + location


def ProjectLocationCluster(project, location, cluster):
  return ProjectLocation(project, location) + '/clusters/' + cluster


def ProjectLocationClusterNodePool(project, location, cluster, nodepool):
  return (ProjectLocationCluster(project, location, cluster) + '/nodePools/' +
          nodepool)


def ProjectLocationOperation(project, location, operation):
  return ProjectLocation(project, location) + '/operations/' + operation
