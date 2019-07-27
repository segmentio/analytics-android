# -*- coding: utf-8 -*- #
# Copyright 2017 Google LLC. All Rights Reserved.
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
"""Command for updating networks."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.compute import base_classes
from googlecloudsdk.api_lib.compute.operations import poller
from googlecloudsdk.api_lib.util import waiter
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.compute.networks import flags
from googlecloudsdk.command_lib.compute.networks import network_utils
from googlecloudsdk.core import log
from googlecloudsdk.core import resources
from googlecloudsdk.core.console import console_io


@base.ReleaseTracks(base.ReleaseTrack.BETA, base.ReleaseTrack.GA)
class Update(base.UpdateCommand):
  """Update a Google Compute Engine Network."""

  NETWORK_ARG = None

  @classmethod
  def Args(cls, parser):
    cls.NETWORK_ARG = flags.NetworkArgument()
    cls.NETWORK_ARG.AddArgument(parser)
    base.ASYNC_FLAG.AddToParser(parser)
    network_utils.AddUpdateArgs(parser)

  def Run(self, args):
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    messages = holder.client.messages
    service = holder.client.apitools_client.networks

    network_ref = self.NETWORK_ARG.ResolveAsResource(args, holder.resources)

    if args.switch_to_custom_subnet_mode:
      prompt_msg = 'Network [{0}] will be switched to custom mode. '.format(
          network_ref.Name()) + 'This operation cannot be undone.'
      console_io.PromptContinue(
          message=prompt_msg, default=True, cancel_on_no=True)
      result = service.SwitchToCustomMode(
          messages.ComputeNetworksSwitchToCustomModeRequest(
              project=network_ref.project, network=network_ref.Name()))
      operation_ref = resources.REGISTRY.Parse(
          result.name,
          params={'project': network_ref.project},
          collection='compute.globalOperations')

      if args.async:
        log.UpdatedResource(
            operation_ref,
            kind='network {0}'.format(network_ref.Name()),
            is_async=True,
            details='Run the [gcloud compute operations describe] command '
            'to check the status of this operation.')
        return result

      operation_poller = poller.Poller(service, network_ref)
      return waiter.WaitFor(operation_poller, operation_ref,
                            'Switching network to custom-mode')

    if args.bgp_routing_mode or getattr(args, 'multicast_mode', None):
      network_resource = messages.Network()
      if args.bgp_routing_mode:
        network_resource.routingConfig = messages.NetworkRoutingConfig()
        network_resource.routingConfig.routingMode = (
            messages.NetworkRoutingConfig.RoutingModeValueValuesEnum(
                args.bgp_routing_mode.upper()))
      if getattr(args, 'multicast_mode', None):
        network_resource.multicastMode = (
            messages.Network.MulticastModeValueValuesEnum(
                args.multicast_mode.upper()))
      resource = service.Patch(
          messages.ComputeNetworksPatchRequest(
              project=network_ref.project,
              network=network_ref.Name(),
              networkResource=network_resource))

    return resource


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class UpdateAlpha(Update):
  """Update a Google Compute Engine Network."""

  @classmethod
  def Args(cls, parser):
    cls.NETWORK_ARG = flags.NetworkArgument()
    cls.NETWORK_ARG.AddArgument(parser)
    base.ASYNC_FLAG.AddToParser(parser)
    network_utils.AddUpdateArgsAlpha(parser)


Update.detailed_help = {
    'brief':
        'Update a Google Compute Engine network',
    'DESCRIPTION':
        """\

        *{command}* is used to update Google Compute Engine networks."""
}
