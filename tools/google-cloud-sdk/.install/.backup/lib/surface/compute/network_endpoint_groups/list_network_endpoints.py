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
"""list endpoints command."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from apitools.base.py import list_pager
from googlecloudsdk.api_lib.compute import base_classes
from googlecloudsdk.api_lib.compute import filter_rewrite
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.compute import flags as compute_flags
from googlecloudsdk.command_lib.compute.network_endpoint_groups import flags


@base.ReleaseTracks(base.ReleaseTrack.BETA, base.ReleaseTrack.GA)
class ListEndpoints(base.ListCommand):
  r"""List network endpoints in a network endpoint group.

  ## EXAMPLES

  To list network endpoints of a network endpoint group:

    $ {command} my-neg --zone=us-central1-a
  """

  @staticmethod
  def Args(parser):
    parser.display_info.AddFormat("""\
        table(
          networkEndpoint.instance,
          networkEndpoint.ipAddress,
          networkEndpoint.port
        )""")
    base.URI_FLAG.RemoveFromParser(parser)
    flags.MakeNetworkEndpointGroupsArg().AddArgument(parser)

  def Run(self, args):
    return self._Run(args)

  def _Run(self, args, support_global_scope=False):
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    client = holder.client
    messages = client.messages

    neg_ref = flags.MakeNetworkEndpointGroupsArg(
        support_global_scope=support_global_scope).ResolveAsResource(
            args,
            holder.resources,
            scope_lister=compute_flags.GetDefaultScopeLister(client))

    args.filter, filter_expr = filter_rewrite.Rewriter().Rewrite(args.filter)

    if hasattr(neg_ref, 'zone'):
      request = messages.ComputeNetworkEndpointGroupsListNetworkEndpointsRequest(
          networkEndpointGroup=neg_ref.Name(),
          project=neg_ref.project,
          zone=neg_ref.zone,
          filter=filter_expr)
      service = client.apitools_client.networkEndpointGroups
    else:
      request = messages.ComputeGlobalNetworkEndpointGroupsListNetworkEndpointsRequest(
          networkEndpointGroup=neg_ref.Name(),
          project=neg_ref.project,
          filter=filter_expr)
      service = client.apitools_client.globalNetworkEndpointGroups

    return list_pager.YieldFromList(
        service,
        request,
        method='ListNetworkEndpoints',
        field='items',
        limit=args.limit,
        batch_size=None)


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class AlphaListEndpoints(ListEndpoints):
  r"""List network endpoints in a network endpoint group.

  ## EXAMPLES

  To list network endpoints of a network endpoint group:

    $ {command} my-neg --zone=us-central1-a
  """

  @staticmethod
  def Args(parser):
    parser.display_info.AddFormat("""\
        table(
          networkEndpoint.instance,
          networkEndpoint.ipAddress,
          networkEndpoint.port,
          networkEndpoint.fqdn
        )""")
    base.URI_FLAG.RemoveFromParser(parser)
    flags.MakeNetworkEndpointGroupsArg(
        support_global_scope=True).AddArgument(parser)

  def Run(self, args):
    return self._Run(args, support_global_scope=True)
