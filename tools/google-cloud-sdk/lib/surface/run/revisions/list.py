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
"""Command for listing available reivions."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.run import commands
from googlecloudsdk.command_lib.run import connection_context
from googlecloudsdk.command_lib.run import flags
from googlecloudsdk.command_lib.run import pretty_print
from googlecloudsdk.command_lib.run import resource_args
from googlecloudsdk.command_lib.run import serverless_operations
from googlecloudsdk.command_lib.util.concepts import concept_parsers
from googlecloudsdk.command_lib.util.concepts import presentation_specs


@base.ReleaseTracks(base.ReleaseTrack.BETA)
class List(commands.List):
  """List available revisions."""

  detailed_help = {
      'DESCRIPTION': """\
          {description}
          """,
      'EXAMPLES': """\
          To list all revisions for the provided service:

              $ {command} --service foo
         """,
  }

  @classmethod
  def CommonArgs(cls, parser):
    # Flags specific to managed CR
    managed_group = flags.GetManagedArgGroup(parser)
    flags.AddRegionArgWithDefault(managed_group)
    # Flags specific to CRoGKE
    gke_group = flags.GetGkeArgGroup(parser)
    namespace_presentation = presentation_specs.ResourcePresentationSpec(
        '--namespace',
        resource_args.GetNamespaceResourceSpec(),
        'Namespace to list services in.',
        required=True,
        prefixes=False)
    concept_parsers.ConceptParser(
        [resource_args.CLUSTER_PRESENTATION,
         namespace_presentation]).AddToParser(gke_group)
    # Flags not specific to any platform
    flags.AddServiceFlag(parser)
    parser.display_info.AddFormat(
        'table('
        '{ready_column},'
        'name:label=REVISION,service_name:label=SERVICE,author,'
        'creation_timestamp.date("%Y-%m-%d %H:%M:%S %Z"):label=CREATED)'.format(
            ready_column=pretty_print.READY_COLUMN))
    parser.display_info.AddUriFunc(cls._GetResourceUri)

  @classmethod
  def Args(cls, parser):
    cls.CommonArgs(parser)
    # Flags not specific to any platform
    flags.AddPlatformArg(parser)

  def Run(self, args):
    """List available revisions."""
    service_name = args.service
    conn_context = connection_context.GetConnectionContext(args)
    namespace_ref = args.CONCEPTS.namespace.Parse()
    with serverless_operations.Connect(conn_context) as client:
      self.SetCompleteApiEndpoint(conn_context.endpoint)
      return client.ListRevisions(namespace_ref, service_name)


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class AlphaList(List):
  """List available revisions."""

  @classmethod
  def Args(cls, parser):
    cls.CommonArgs(parser)
    # Flags specific to connecting to a Kubernetes cluster (kubeconfig)
    kubernetes_group = flags.GetKubernetesArgGroup(parser)
    flags.AddKubeconfigFlags(kubernetes_group)
    # Flags not specific to any platform
    flags.AddAlphaPlatformArg(parser)

AlphaList.__doc__ = List.__doc__
