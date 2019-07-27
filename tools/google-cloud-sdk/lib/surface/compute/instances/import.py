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
"""Command for importing instances in OVF format into GCE."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import re

from googlecloudsdk.api_lib.compute import base_classes
from googlecloudsdk.api_lib.compute import daisy_utils
from googlecloudsdk.api_lib.compute import instance_utils
from googlecloudsdk.api_lib.storage import storage_util
from googlecloudsdk.calliope import base
from googlecloudsdk.calliope import exceptions
from googlecloudsdk.command_lib.compute import completers
from googlecloudsdk.command_lib.compute.images import os_choices
from googlecloudsdk.command_lib.compute.instances import flags as instances_flags
from googlecloudsdk.command_lib.compute.sole_tenancy import flags as sole_tenancy_flags
from googlecloudsdk.command_lib.util.args import labels_util
from googlecloudsdk.core import log
from googlecloudsdk.core import properties

_OUTPUT_FILTER = ['[Daisy', '[import-', 'starting build', '  import', 'ERROR']


@base.ReleaseTracks(base.ReleaseTrack.ALPHA, base.ReleaseTrack.BETA)
class Import(base.CreateCommand):
  """Import an instance into Google Compute Engine from OVF."""

  @classmethod
  def Args(cls, parser):
    instances_flags.AddCanIpForwardArgs(parser)
    instances_flags.AddMachineTypeArgs(parser)
    instances_flags.AddNoRestartOnFailureArgs(parser)
    instances_flags.AddTagsArgs(parser)
    instances_flags.AddCustomMachineTypeArgs(parser)
    instances_flags.AddNetworkArgs(parser)
    instances_flags.AddPrivateNetworkIpArgs(parser)
    instances_flags.AddDeletionProtectionFlag(parser)
    instances_flags.AddNetworkTierArgs(parser, instance=True)
    labels_util.AddCreateLabelsFlags(parser)
    daisy_utils.AddCommonDaisyArgs(parser, add_log_location=False)

    instances_flags.INSTANCES_ARG_FOR_IMPORT.AddArgument(
        parser, operation_type='import')

    parser.add_argument(
        '--source-uri',
        required=True,
        help=('Google Cloud Storage path to one of:\n  OVF descriptor\n  '
              'OVA file\n  Directory with OVF package'))

    parser.add_argument(
        '--os',
        required=True,
        choices=sorted(os_choices.OS_CHOICES_INSTANCE_IMPORT_BETA),
        help='Specifies the OS of the image being imported.')

    parser.add_argument(
        '--description',
        help='Specifies a textual description of the instances.')

    parser.add_argument(
        '--guest-environment',
        action='store_true',
        default=True,
        help='Google Guest Environment will be installed on the instance.')

    parser.display_info.AddCacheUpdater(completers.InstancesCompleter)

    sole_tenancy_flags.AddNodeAffinityFlagToParser(parser)

  def _ValidateInstanceName(self, args):
    """Raise an exception if requested instance name is invalid."""
    instance_name_pattern = re.compile('^[a-z]([-a-z0-9]{0,61}[a-z0-9])?$')
    if not instance_name_pattern.match(args.instance_name):
      raise exceptions.InvalidArgumentException(
          'INSTANCE_NAME',
          'Name must start with a lowercase letter followed by up to '
          '63 lowercase letters, numbers, or hyphens, and cannot end '
          'with a hyphen.')

  def _CheckForExistingInstances(self, instance_name, client):
    """Check that the destination instances do not already exist."""

    request = (client.apitools_client.instances, 'Get',
               client.messages.ComputeInstancesGetRequest(
                   instance=instance_name,
                   project=properties.VALUES.core.project.GetOrFail(),
                   zone=properties.VALUES.compute.zone.GetOrFail()))
    errors = []
    client.MakeRequests([request], errors_to_collect=errors)
    if not errors:
      message = 'The instance [{0}] already exists.'.format(instance_name)
      raise exceptions.InvalidArgumentException('INSTANCE_NAME', message)

  def Run(self, args):
    compute_holder = base_classes.ComputeApiHolder(self.ReleaseTrack())

    self._ValidateInstanceName(args)
    self._CheckForExistingInstances(args.instance_name, compute_holder.client)

    instances_flags.ValidateNicFlags(args)
    instances_flags.ValidateNetworkTierArgs(args)

    log.warning('Importing OVF. This may take 40 minutes for smaller OVFs '
                'and up to a couple of hours for larger OVFs.')

    machine_type = None
    if args.machine_type or args.custom_cpu or args.custom_memory:
      machine_type = instance_utils.InterpretMachineType(
          machine_type=args.machine_type,
          custom_cpu=args.custom_cpu,
          custom_memory=args.custom_memory,
          ext=getattr(args, 'custom_extensions', None),
          vm_gen=getattr(args, 'custom_vm_gen', None))

    try:
      source_uri = daisy_utils.MakeGcsObjectOrPathUri(args.source_uri)
    except storage_util.InvalidObjectNameError:
      raise exceptions.InvalidArgumentException(
          'source-uri',
          'must be a path to an object or a directory in Google Cloud Storage')

    return daisy_utils.RunOVFImportBuild(
        args=args,
        compute_client=compute_holder.client,
        instance_name=args.instance_name,
        source_uri=source_uri,
        no_guest_environment=not args.guest_environment,
        can_ip_forward=args.can_ip_forward,
        deletion_protection=args.deletion_protection,
        description=args.description,
        labels=args.labels,
        machine_type=machine_type,
        network=args.network,
        network_tier=args.network_tier,
        subnet=args.subnet,
        private_network_ip=args.private_network_ip,
        no_restart_on_failure=not args.restart_on_failure,
        os=args.os,
        tags=args.tags,
        zone=properties.VALUES.compute.zone.Get(),
        project=args.project,
        output_filter=_OUTPUT_FILTER,
    )


Import.detailed_help = {
    'brief': (
        'create Google Compute Engine virtual machine instances from virtual '
        'appliance in OVA/OVF format.'),
    'DESCRIPTION':
        """\
        *{command}* creates Google Compute Engine virtual machine instances from
        virtual appliance in OVA/OVF format.

        Importing OVF involves:
        *  Unpacking OVF package (if in OVA format) to Cloud Storage.
        *  Import disks from OVF to Google Compute Engine.
        *  Translate the boot disk to make it bootable in Google Compute Engine.
        *  Create a VM instance using OVF metadata and imported disks and boot it.

        Virtual machine instances, images and disks in Compute engine and files
        stored on Cloud Storage incur charges. See [](https://cloud.google.com/compute/docs/images/importing-virtual-disks#resource_cleanup).
        """,
    'EXAMPLES':
        """\
        To import an OVF package from Google Could Storage into a VM named `my-instance`, run:

          $ {command} my-instance --source-uri=gs://my-bucket/my-dir
        """,
}
