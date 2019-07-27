# -*- coding: utf-8 -*- #
# Copyright 2019 Google Inc. All Rights Reserved.
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
"""Command for getting the effective firewalls applied on instance network interfaces."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.compute import base_classes
from googlecloudsdk.calliope import base
from googlecloudsdk.calliope import exceptions
from googlecloudsdk.command_lib.compute import flags
from googlecloudsdk.command_lib.compute.instances import flags as instances_flags


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class GetEffectiveFirewalls(base.Command):
  r"""Get the effective firewalls on a Google Compute Engine virtual machine network interface.

  *{command}* Get the effective firewalls applied on the network interfaces of
  a Google Compute Engine virtual machine. For example:

    $ {command} example-instance --zone us-central1-a

  gets the effective firewalls applied on the default network interface of a
  Google Compute Engine virtual machine "example-instance" in zone
  us-central1-a
  """

  @staticmethod
  def Args(parser):
    instances_flags.INSTANCE_ARG.AddArgument(parser)
    parser.add_argument(
        '--network-interface',
        default='nic0',
        help='The name of the network interface to get the effective firewalls.'
    )

  def Run(self, args):
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    client = holder.client
    messages = holder.client.messages

    instance_ref = instances_flags.INSTANCE_ARG.ResolveAsResource(
        args, holder.resources,
        scope_lister=flags.GetDefaultScopeLister(holder.client))

    instance = client.apitools_client.instances.Get(
        messages.ComputeInstancesGetRequest(**instance_ref.AsDict()))
    for i in instance.networkInterfaces:
      if i.name == args.network_interface:
        break
    else:
      raise exceptions.UnknownArgumentException(
          'network-interface',
          'Instance does not have a network interface [{}], '
          'present interfaces are [{}].'.format(
              args.network_interface, ', '.join(
                  [i.name for i in instance.networkInterfaces])))

    request = messages.ComputeInstancesGetEffectiveFirewallsRequest(
        project=instance_ref.project,
        instance=instance_ref.instance,
        zone=instance_ref.zone,
        networkInterface=args.network_interface)
    return client.apitools_client.instances.GetEffectiveFirewalls(request)
