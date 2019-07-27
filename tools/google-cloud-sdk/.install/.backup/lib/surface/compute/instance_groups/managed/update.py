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
"""Command for updating managed instance group."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.compute import base_classes
from googlecloudsdk.api_lib.compute import managed_instance_groups_utils
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.compute import flags
from googlecloudsdk.command_lib.compute import scope as compute_scope
from googlecloudsdk.command_lib.compute.instance_groups import flags as instance_groups_flags
from googlecloudsdk.command_lib.compute.instance_groups.flags import AutoDeleteFlag
from googlecloudsdk.command_lib.compute.managed_instance_groups import auto_healing_utils
import six


@base.ReleaseTracks(base.ReleaseTrack.GA)
class UpdateGA(base.UpdateCommand):
  r"""Update Google Compute Engine managed instance groups.

  *{command}* allows you to specify or modify AutoHealingPolicy for an existing
  managed instance group.

  When updating the AutoHealingPolicy, you may specify the health check, initial
  delay, or both. If the field is unspecified, its value won't be modified. If
  `--health-check` is specified, the health check will be used to monitor the
  health of your application. Whenever the health check signal for the instance
  becomes `UNHEALTHY`, the autohealing action (`RECREATE`) on an instance will
  be performed.

  If no health check is specified, the instance autohealing will be triggered by
  the instance status only (i.e. the autohealing action (`RECREATE`) on an
  instance will be performed if `instance.status` is not `RUNNING`).
  """

  @staticmethod
  def Args(parser):
    instance_groups_flags.MULTISCOPE_INSTANCE_GROUP_MANAGER_ARG.AddArgument(
        parser, operation_type='update')

    autohealing_group = parser.add_mutually_exclusive_group()
    autohealing_group.add_argument(
        '--clear-autohealing',
        action='store_true',
        default=None,
        help="""\
        Clears all autohealing policy fields for the managed instance group.
        """)
    autohealing_params_group = autohealing_group.add_group()
    auto_healing_utils.AddAutohealingArgs(autohealing_params_group)

  def _GetValidatedAutohealingPolicies(self, holder, client, args,
                                       igm_resource):
    health_check = managed_instance_groups_utils.GetHealthCheckUri(
        holder.resources, args)
    auto_healing_policies = (
        managed_instance_groups_utils.ModifyAutohealingPolicies(
            igm_resource.autoHealingPolicies, client.messages, args,
            health_check))
    managed_instance_groups_utils.ValidateAutohealingPolicies(
        auto_healing_policies)
    return auto_healing_policies

  def _MakePatchRequest(self, client, igm_ref, igm_updated_resource):
    if igm_ref.Collection() == 'compute.instanceGroupManagers':
      service = client.apitools_client.instanceGroupManagers
      request = client.messages.ComputeInstanceGroupManagersPatchRequest(
          instanceGroupManager=igm_ref.Name(),
          instanceGroupManagerResource=igm_updated_resource,
          project=igm_ref.project,
          zone=igm_ref.zone)
    else:
      service = client.apitools_client.regionInstanceGroupManagers
      request = client.messages.ComputeRegionInstanceGroupManagersPatchRequest(
          instanceGroupManager=igm_ref.Name(),
          instanceGroupManagerResource=igm_updated_resource,
          project=igm_ref.project,
          region=igm_ref.region)
    return client.MakeRequests([(service, 'Patch', request)])

  def Run(self, args):
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    client = holder.client
    igm_ref = (instance_groups_flags.MULTISCOPE_INSTANCE_GROUP_MANAGER_ARG
               .ResolveAsResource)(
                   args,
                   holder.resources,
                   default_scope=compute_scope.ScopeEnum.ZONE,
                   scope_lister=flags.GetDefaultScopeLister(client))

    if igm_ref.Collection() not in [
        'compute.instanceGroupManagers', 'compute.regionInstanceGroupManagers'
    ]:
      raise ValueError('Unknown reference type {0}'.format(
          igm_ref.Collection()))

    igm_resource = managed_instance_groups_utils.GetInstanceGroupManagerOrThrow(
        igm_ref, client)

    auto_healing_policies = self._GetValidatedAutohealingPolicies(
        holder, client, args, igm_resource)

    if auto_healing_policies is not None:
      return self._MakePatchRequest(
          client, igm_ref,
          client.messages.InstanceGroupManager(
              autoHealingPolicies=auto_healing_policies))


@base.ReleaseTracks(base.ReleaseTrack.BETA)
class UpdateBeta(UpdateGA):
  r"""Update Google Compute Engine managed instance groups.

  *{command}* allows you to specify or modify AutoHealingPolicy for an existing
  managed instance group.

  When updating the AutoHealingPolicy, you may specify the health check, initial
  delay, or both. If the field is unspecified, its value won't be modified. If
  `--health-check` is specified, the health check will be used to monitor the
  health of your application. Whenever the health check signal for the instance
  becomes `UNHEALTHY`, the autohealing action (`RECREATE`) on an instance will
  be performed.

  If no health check is specified, the instance autohealing will be triggered by
  the instance status only (i.e. the autohealing action (`RECREATE`) on an
  instance will be performed if `instance.status` is not `RUNNING`).
  """

  @staticmethod
  def Args(parser):
    UpdateGA.Args(parser)
    instance_groups_flags.AddMigInstanceRedistributionTypeFlag(parser)

  def Run(self, args):
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    client = holder.client
    igm_ref = (instance_groups_flags.MULTISCOPE_INSTANCE_GROUP_MANAGER_ARG
               .ResolveAsResource)(
                   args,
                   holder.resources,
                   default_scope=compute_scope.ScopeEnum.ZONE,
                   scope_lister=flags.GetDefaultScopeLister(client))

    if igm_ref.Collection() not in [
        'compute.instanceGroupManagers', 'compute.regionInstanceGroupManagers'
    ]:
      raise ValueError('Unknown reference type {0}'.format(
          igm_ref.Collection()))

    instance_groups_flags.ValidateMigInstanceRedistributionTypeFlag(
        args.GetValue('instance_redistribution_type'), igm_ref)

    igm_resource = managed_instance_groups_utils.GetInstanceGroupManagerOrThrow(
        igm_ref, client)

    update_policy = (managed_instance_groups_utils
                     .ApplyInstanceRedistributionTypeToUpdatePolicy)(
                         client, args.GetValue('instance_redistribution_type'),
                         igm_resource.updatePolicy)

    auto_healing_policies = self._GetValidatedAutohealingPolicies(
        holder, client, args, igm_resource)

    igm_updated_resource = client.messages.InstanceGroupManager(
        updatePolicy=update_policy)
    if auto_healing_policies is not None:
      igm_updated_resource.autoHealingPolicies = auto_healing_policies
    return self._MakePatchRequest(client, igm_ref, igm_updated_resource)


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class UpdateAlpha(UpdateBeta):
  r"""Update Google Compute Engine managed instance groups.

  *{command}* allows you to specify or modify the StatefulPolicy and
  AutoHealingPolicy for an existing managed instance group.

  Stateful Policy defines what stateful resources should be preserved for the
  group. When instances in the group are removed or recreated, those stateful
  properties are always applied to them. This command allows you to change the
  preserved resources by adding more disks or removing existing disks and allows
  you to turn on and off preserving instance names.

  When updating the AutoHealingPolicy, you may specify the health check, initial
  delay, or both. If the field is unspecified, its value won't be modified. If
  `--health-check` is specified, the health check will be used to monitor the
  health of your application. Whenever the health check signal for the instance
  becomes `UNHEALTHY`, the autohealing action (`RECREATE`) on an instance will
  be performed.

  If no health check is specified, the instance autohealing will be triggered by
  the instance status only (i.e. the autohealing action (`RECREATE`) on an
  instance will be performed if `instance.status` is not `RUNNING`).
  """

  @staticmethod
  def Args(parser):
    UpdateBeta.Args(parser)
    instance_groups_flags.AddMigUpdateStatefulFlags(parser)

  def _MakePreservedStateDiskEntry(self, client, stateful_disk_dict):
    """Create StatefulPolicyPreservedState from a list of device names."""
    auto_delete = (stateful_disk_dict.get('auto-delete') or
                   AutoDeleteFlag.NEVER).GetAutoDeleteEnumValue(
                       client.messages.StatefulPolicyPreservedStateDiskDevice
                       .AutoDeleteValueValuesEnum)
    disk_device = client.messages.StatefulPolicyPreservedStateDiskDevice(
        autoDelete=auto_delete)
    # Add all disk_devices to map
    return client.messages.StatefulPolicyPreservedState.DisksValue \
        .AdditionalProperty(
            key=stateful_disk_dict.get('device-name'), value=disk_device)

  def _UpdateStatefulPolicy(self, client, current_stateful_policy, update_disks,
                            remove_device_names):
    """Create an updated stateful policy with the updated disk data and removed disks as specified."""
    if not update_disks:
      update_disks = []
    if not remove_device_names:
      remove_device_names = []
    update_map = {
        update_disk.get('device-name'): update_disk
        for update_disk in update_disks
    }
    additional_properties = []
    if current_stateful_policy and current_stateful_policy.preservedState \
        and current_stateful_policy.preservedState.disks:
      current_disks = current_stateful_policy\
        .preservedState.disks.additionalProperties
    else:
      current_disks = []
    for disk_entry in current_disks:
      if disk_entry.key in remove_device_names:
        continue
      if disk_entry.key not in update_map:
        additional_properties.append(disk_entry)
    for _, stateful_disk in six.iteritems(update_map):
      additional_properties.append(
          self._MakePreservedStateDiskEntry(client, stateful_disk))
    additional_properties.sort(key=lambda x: x.key)
    if additional_properties:
      return client.messages.StatefulPolicy(
          preservedState=client.messages.StatefulPolicyPreservedState(
              disks=client.messages.StatefulPolicyPreservedState.DisksValue(
                  additionalProperties=additional_properties)))
    else:
      return client.messages.StatefulPolicy()

  def _MakeUpdateRequest(self, client, igm_ref, igm_updated_resource):
    if igm_ref.Collection() == 'compute.instanceGroupManagers':
      service = client.apitools_client.instanceGroupManagers
      request = client.messages.ComputeInstanceGroupManagersUpdateRequest(
          instanceGroupManager=igm_ref.Name(),
          instanceGroupManagerResource=igm_updated_resource,
          project=igm_ref.project,
          zone=igm_ref.zone)
    else:
      service = client.apitools_client.regionInstanceGroupManagers
      request = client.messages.ComputeRegionInstanceGroupManagersUpdateRequest(
          instanceGroupManager=igm_ref.Name(),
          instanceGroupManagerResource=igm_updated_resource,
          project=igm_ref.project,
          region=igm_ref.region)
    return client.MakeRequests([(service, 'Update', request)])

  def _StatefulArgsSet(self, args):
    return (args.IsSpecified('stateful_names') or
            args.IsSpecified('update_stateful_disk') or
            args.IsSpecified('remove_stateful_disks'))

  def _StatefulnessIntroduced(self, args):
    return (args.IsSpecified('stateful_names') or
            args.IsSpecified('update_stateful_disk'))

  def Run(self, args):
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    client = holder.client
    igm_ref = (instance_groups_flags.MULTISCOPE_INSTANCE_GROUP_MANAGER_ARG
               .ResolveAsResource)(
                   args,
                   holder.resources,
                   default_scope=compute_scope.ScopeEnum.ZONE,
                   scope_lister=flags.GetDefaultScopeLister(client))

    if igm_ref.Collection() not in [
        'compute.instanceGroupManagers', 'compute.regionInstanceGroupManagers'
    ]:
      raise ValueError('Unknown reference type {0}'.format(
          igm_ref.Collection()))

    instance_groups_flags.ValidateMigInstanceRedistributionTypeFlag(
        args.GetValue('instance_redistribution_type'), igm_ref)

    igm_resource = managed_instance_groups_utils.GetInstanceGroupManagerOrThrow(
        igm_ref, client)
    if self._StatefulnessIntroduced(args):
      managed_instance_groups_utils.ValidateIgmReadyForStatefulness(
          igm_resource, client)

    device_names = instance_groups_flags.ValidateUpdateStatefulPolicyParams(
        args, igm_resource.statefulPolicy)

    update_policy = (managed_instance_groups_utils
                     .ApplyInstanceRedistributionTypeToUpdatePolicy)(
                         client, args.GetValue('instance_redistribution_type'),
                         igm_resource.updatePolicy)

    auto_healing_policies = self._GetValidatedAutohealingPolicies(
        holder, client, args, igm_resource)

    if not self._StatefulArgsSet(args):
      igm_updated_resource = client.messages.InstanceGroupManager(
          updatePolicy=update_policy)
      if auto_healing_policies is not None:
        igm_updated_resource.autoHealingPolicies = auto_healing_policies
      return self._MakePatchRequest(client, igm_ref, igm_updated_resource)

    if not device_names:
      # TODO(b/70314588): Use Patch instead of manual Update.
      if args.IsSpecified(
          'stateful_names') and not args.GetValue('stateful_names'):
        igm_resource.reset('statefulPolicy')
      elif igm_resource.statefulPolicy or args.GetValue('stateful_names'):
        igm_resource.statefulPolicy = self._UpdateStatefulPolicy(
            client, igm_resource.statefulPolicy, args.update_stateful_disk,
            args.remove_stateful_disks)
      igm_resource.updatePolicy = update_policy
      if auto_healing_policies is not None:
        igm_resource.autoHealingPolicies = auto_healing_policies
      return self._MakeUpdateRequest(client, igm_ref, igm_resource)

    stateful_policy = self._UpdateStatefulPolicy(
        client, igm_resource.statefulPolicy, args.update_stateful_disk,
        args.remove_stateful_disks)
    igm_updated_resource = client.messages.InstanceGroupManager(
        statefulPolicy=stateful_policy, updatePolicy=update_policy)
    if auto_healing_policies is not None:
      igm_updated_resource.autoHealingPolicies = auto_healing_policies

    return self._MakePatchRequest(client, igm_ref, igm_updated_resource)
