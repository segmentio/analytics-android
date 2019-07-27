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
"""Create network endpoint group command."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.compute import base_classes
from googlecloudsdk.api_lib.compute import network_endpoint_groups
from googlecloudsdk.calliope import base
from googlecloudsdk.calliope import exceptions
from googlecloudsdk.command_lib.compute import flags as compute_flags
from googlecloudsdk.command_lib.compute import scope as compute_scope
from googlecloudsdk.command_lib.compute.network_endpoint_groups import flags
from googlecloudsdk.core import log


def _Run(args, holder, support_global_scope=False):
  """Issues the request necessary for adding the network endpoint group."""
  client = holder.client
  messages = holder.client.messages
  resources = holder.resources
  neg_client = network_endpoint_groups.NetworkEndpointGroupsClient(
      client, messages, resources)
  neg_ref = flags.MakeNetworkEndpointGroupsArg(
      support_global_scope).ResolveAsResource(
          args,
          holder.resources,
          default_scope=compute_scope.ScopeEnum.ZONE,
          scope_lister=compute_flags.GetDefaultScopeLister(holder.client))

  _ValidateNEG(args, neg_ref)

  result = neg_client.Create(
      neg_ref,
      args.network_endpoint_type,
      default_port=args.default_port,
      network=args.network,
      subnet=args.subnet)
  log.CreatedResource(neg_ref.Name(), 'network endpoint group')
  return result


def _ValidateNEG(args, neg_ref):
  """Validate NEG input before making request."""
  is_zonal = hasattr(neg_ref, 'zone')
  network_endpoint_type = args.network_endpoint_type

  if is_zonal:
    if network_endpoint_type == 'internet-ip-port' or network_endpoint_type == 'internet-fqdn-port':
      raise exceptions.InvalidArgumentException(
          '--network-endpoint-type',
          'Internet network endpoint types not supported for zonal NEGs.')
  else:
    if network_endpoint_type == 'gce-vm-ip-port':
      raise exceptions.InvalidArgumentException(
          '--network-endpoint-type',
          'GCE_VM_IP_PORT network endpoint type not supported for global NEGs.')
    if args.network is not None:
      raise exceptions.InvalidArgumentException(
          '--network', 'Global NEGs cannot specify network.')


@base.ReleaseTracks(base.ReleaseTrack.BETA, base.ReleaseTrack.GA)
class Create(base.CreateCommand):
  r"""Create a Google Compute Engine network endpoint group.

  ## EXAMPLES

  To create a network endpoint group:

    $ {command} my-neg --zone=us-central1-a --network=my-network \
    --subnet=my-subnetwork
  """

  @staticmethod
  def Args(parser, support_neg_type=False):
    flags.MakeNetworkEndpointGroupsArg().AddArgument(parser)
    flags.AddCreateNegArgsToParser(parser, support_neg_type=support_neg_type)

  def Run(self, args):
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    return _Run(args, holder)


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class CreateAlpha(Create):
  r"""Create a Google Compute Engine network endpoint group.

  ## EXAMPLES

  To create a network endpoint group:

    $ {command} my-neg --zone=us-central1-a --network=my-network \
    --subnet=my-subnetwork
  """

  @staticmethod
  def Args(parser):
    flags.MakeNetworkEndpointGroupsArg(
        support_global_scope=True).AddArgument(parser)
    flags.AddCreateNegArgsToParser(
        parser, support_neg_type=True, support_global_scope=True)

  def Run(self, args):
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    return _Run(args, holder, support_global_scope=True)
