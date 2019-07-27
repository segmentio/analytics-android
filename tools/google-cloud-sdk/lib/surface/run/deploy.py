# -*- coding: utf-8 -*- #
# Copyright 2018 Google LLC. All Rights Reserved.
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
"""Deploy a container to Cloud Run."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.run import config_changes as config_changes_mod
from googlecloudsdk.command_lib.run import connection_context
from googlecloudsdk.command_lib.run import flags
from googlecloudsdk.command_lib.run import pretty_print
from googlecloudsdk.command_lib.run import resource_args
from googlecloudsdk.command_lib.run import serverless_operations
from googlecloudsdk.command_lib.run import stages
from googlecloudsdk.command_lib.util.args import labels_util
from googlecloudsdk.command_lib.util.concepts import concept_parsers
from googlecloudsdk.command_lib.util.concepts import presentation_specs
from googlecloudsdk.core.console import progress_tracker


@base.ReleaseTracks(base.ReleaseTrack.BETA)
class Deploy(base.Command):
  """Deploy a container to Cloud Run."""

  detailed_help = {
      'DESCRIPTION': """\
          Deploys container images to Google Cloud Run.
          """,
      'EXAMPLES': """\
          To deploy a container to the service `my-backend` on Cloud Run:

              $ {command} my-backend --image gcr.io/my/image

          You may also omit the service name. Then a prompt will be displayed
          with a suggested default value:

              $ {command} --image gcr.io/my/image

          To deploy to Cloud Run on Kubernetes Engine, you need to specify a cluster:

              $ {command} --image gcr.io/my/image --cluster my-cluster
          """,
  }

  @staticmethod
  def CommonArgs(parser):
    # Flags specific to managed CR
    managed_group = flags.GetManagedArgGroup(parser)
    flags.AddRegionArg(managed_group)
    flags.AddAllowUnauthenticatedFlag(managed_group)
    flags.AddRevisionSuffixArg(managed_group)
    flags.AddServiceAccountFlag(managed_group)
    flags.AddCloudSQLFlags(managed_group)
    # Flags specific to CRoGKE
    gke_group = flags.GetGkeArgGroup(parser)
    concept_parsers.ConceptParser([resource_args.CLUSTER_PRESENTATION
                                  ]).AddToParser(gke_group)
    # Flags not specific to any platform
    service_presentation = presentation_specs.ResourcePresentationSpec(
        'SERVICE',
        resource_args.GetServiceResourceSpec(prompt=True),
        'Service to deploy to.',
        required=True,
        prefixes=False)
    flags.AddSourceRefFlags(parser)
    flags.AddFunctionArg(parser)
    flags.AddMutexEnvVarsFlags(parser)
    flags.AddMemoryFlag(parser)
    flags.AddConcurrencyFlag(parser)
    flags.AddTimeoutFlag(parser)
    flags.AddAsyncFlag(parser)
    concept_parsers.ConceptParser([service_presentation]).AddToParser(parser)

  @staticmethod
  def Args(parser):
    Deploy.CommonArgs(parser)
    # Flags specific to CRoGKE
    gke_group = flags.GetGkeArgGroup(parser)
    flags.AddEndpointVisibilityEnum(gke_group)
    flags.AddCpuFlag(gke_group)
    # Flags not specific to any platform
    flags.AddPlatformArg(parser)

  def Run(self, args):
    """Deploy a container to Cloud Run."""
    image = args.image

    conn_context = connection_context.GetConnectionContext(args)
    config_changes = flags.GetConfigurationChanges(args)

    service_ref = flags.GetService(args)

    with serverless_operations.Connect(conn_context) as operations:
      image_change = config_changes_mod.ImageChange(image)
      changes = [image_change]
      if config_changes:
        changes.extend(config_changes)
      endpoint_visibility = flags.GetEndpointVisibility(args)
      exists = operations.GetService(service_ref)
      allow_unauth = None
      if conn_context.supports_one_platform:
        allow_unauth = flags.GetAllowUnauthenticated(args,
                                                     operations,
                                                     service_ref,
                                                     not exists)
        # Don't try to remove a policy binding from a service that doesn't exist
        if not exists and not allow_unauth:
          allow_unauth = None

      msg = ('Deploying {dep_type} to {operator} '
             'service [{{bold}}{service}{{reset}}]'
             ' in {ns_label} [{{bold}}{ns}{{reset}}]')
      msg += conn_context.location_label

      pretty_print.Info(msg.format(
          operator=conn_context.operator,
          ns_label=conn_context.ns_label,
          dep_type='container',
          service=service_ref.servicesId,
          ns=service_ref.namespacesId))

      deployment_stages = stages.ServiceStages(allow_unauth is not None)
      header = 'Deploying...' if exists else 'Deploying new service...'
      with progress_tracker.StagedProgressTracker(
          header,
          deployment_stages,
          failure_message='Deployment failed',
          suppress_output=args.async) as tracker:
        operations.ReleaseService(
            service_ref,
            changes,
            tracker,
            asyn=args.async,
            private_endpoint=endpoint_visibility,
            allow_unauthenticated=allow_unauth)
      if args.async:
        pretty_print.Success(
            'Service [{{bold}}{serv}{{reset}}] is deploying '
            'asynchronously.'.format(serv=service_ref.servicesId))
      else:
        service = operations.GetService(service_ref)
        msg = (
            'Service [{{bold}}{serv}{{reset}}] '
            'revision [{{bold}}{rev}{{reset}}] '
            'has been deployed and is serving traffic at '
            '{{bold}}{url}{{reset}}')
        msg = msg.format(
            serv=service_ref.servicesId,
            rev=service.status.latestReadyRevisionName,
            url=service.domain)
        pretty_print.Success(msg)


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class AlphaDeploy(Deploy):
  """Deploy a container to Cloud Run."""

  @staticmethod
  def Args(parser):
    Deploy.CommonArgs(parser)
    labels_util.AddCreateLabelsFlags(parser)
    labels_util.AddUpdateLabelsFlags(parser)
    # Flags specific to connecting to a Kubernetes cluster (kubeconfig)
    kubernetes_group = flags.GetKubernetesArgGroup(parser)
    flags.AddKubeconfigFlags(kubernetes_group)
    # Flags specific to connecting to a cluster
    cluster_group = flags.GetClusterArgGroup(parser)
    flags.AddEndpointVisibilityEnum(cluster_group)
    flags.AddCpuFlag(cluster_group)
    # Flags not specific to any platform
    flags.AddAlphaPlatformArg(parser)
    flags.AddSecretsFlags(parser)
    flags.AddConfigMapsFlags(parser)

AlphaDeploy.__doc__ = Deploy.__doc__
