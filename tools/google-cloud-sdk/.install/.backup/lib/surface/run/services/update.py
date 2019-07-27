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
"""Command for updating env vars and other configuration info."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.run import connection_context
from googlecloudsdk.command_lib.run import exceptions
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
class Update(base.Command):
  """Update Cloud Run environment variables and other configuration settings.
  """

  detailed_help = {
      'DESCRIPTION': """\
          {description}
          """,
      'EXAMPLES': """\
          To update one or more env vars:

              $ {command} myservice --update-env-vars KEY1=VALUE1,KEY2=VALUE2
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
        'Service to update the configuration of.',
        required=True,
        prefixes=False)
    flags.AddMutexEnvVarsFlags(parser)
    flags.AddMemoryFlag(parser)
    flags.AddConcurrencyFlag(parser)
    flags.AddTimeoutFlag(parser)
    flags.AddAsyncFlag(parser)
    concept_parsers.ConceptParser([service_presentation]).AddToParser(parser)

  @staticmethod
  def Args(parser):
    Update.CommonArgs(parser)
    # Flags specific to CRoGKE
    gke_group = flags.GetGkeArgGroup(parser)
    flags.AddEndpointVisibilityEnum(gke_group)
    flags.AddCpuFlag(gke_group)
    # Flags not specific to any platform
    flags.AddPlatformArg(parser)

  def Run(self, args):
    """Update configuration information about the service.

    Does not change the running code.

    Args:
      args: Args!
    """
    changes = flags.GetConfigurationChanges(args)
    endpoint_visibility = flags.GetEndpointVisibility(args)
    allow_unauth = flags.GetAllowUnauthenticated(args)
    if not changes and endpoint_visibility is None and allow_unauth is None:
      raise exceptions.NoConfigurationChangeError(
          'No configuration change requested. '
          'Did you mean to include the flags `--update-env-vars`, '
          '`--memory`, `--concurrency`, `--timeout`, `--connectivity`, '
          'or `--allow-unauthenticated`?')

    conn_context = connection_context.GetConnectionContext(args)
    service_ref = flags.GetService(args)

    with serverless_operations.Connect(conn_context) as client:
      deployment_stages = stages.ServiceStages(allow_unauth is not None)
      with progress_tracker.StagedProgressTracker(
          'Deploying...',
          deployment_stages,
          failure_message='Deployment failed',
          suppress_output=args.async) as tracker:
        client.ReleaseService(
            service_ref,
            changes,
            tracker,
            asyn=args.async,
            private_endpoint=endpoint_visibility,
            allow_unauthenticated=allow_unauth)
      if args.async:
        pretty_print.Success(
            'Deploying asynchronously.')
      else:
        service = client.GetService(service_ref)
        active_revs = client.GetActiveRevisions(service_ref)

        msg = ('Service [{{bold}}{serv}{{reset}}] revision{plural} {rev_msg} '
               'is active and serving traffic at {{bold}}{url}{{reset}}')

        rev_msg = ' '.join(
            ['[{{bold}}{}{{reset}}]'.format(rev) for rev in active_revs])

        msg = msg.format(
            serv=service_ref.servicesId,
            plural='s' if len(active_revs) > 1 else '',
            rev_msg=rev_msg,
            url=service.domain)

        pretty_print.Success(msg)


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class AlphaUpdate(Update):
  """Update Cloud Run environment variables and other configuration settings.
  """

  @staticmethod
  def Args(parser):
    Update.CommonArgs(parser)
    # Flags specific to managed CR
    managed_group = flags.GetManagedArgGroup(parser)
    flags.AddVpcConnectorArg(managed_group)
    # Flags specific to connecting to a Kubernetes cluster (kubeconfig)
    kubernetes_group = flags.GetKubernetesArgGroup(parser)
    flags.AddKubeconfigFlags(kubernetes_group)
    # Flags specific to connecting to a cluster
    cluster_group = flags.GetClusterArgGroup(parser)
    flags.AddEndpointVisibilityEnum(cluster_group)
    flags.AddCpuFlag(cluster_group)
    # Flags not specific to any platform
    labels_util.AddUpdateLabelsFlags(parser)
    flags.AddAlphaPlatformArg(parser)
    flags.AddSecretsFlags(parser)
    flags.AddConfigMapsFlags(parser)

AlphaUpdate.__doc__ = Update.__doc__
