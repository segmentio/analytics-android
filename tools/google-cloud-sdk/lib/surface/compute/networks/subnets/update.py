# -*- coding: utf-8 -*- #
# Copyright 2016 Google LLC. All Rights Reserved.
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
"""Command for modifying the properties of a subnetwork."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.compute import base_classes
from googlecloudsdk.api_lib.compute import subnets_utils
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.compute.networks.subnets import flags


@base.ReleaseTracks(base.ReleaseTrack.GA)
class Update(base.UpdateCommand):
  """Updates properties of an existing Google Compute Engine subnetwork."""

  _include_alpha_logging = False
  _include_beta_logging = False
  _include_l7_internal_load_balancing = False
  _include_private_ipv6_access = False

  @classmethod
  def Args(cls, parser):
    """The command arguments handler.

    Args:
      parser: An argparse.ArgumentParser instance.
    """
    cls.SUBNETWORK_ARG = flags.SubnetworkArgument()
    cls.SUBNETWORK_ARG.AddArgument(parser, operation_type='update')

    flags.AddUpdateArgs(parser, cls._include_alpha_logging,
                        cls._include_beta_logging,
                        cls._include_l7_internal_load_balancing,
                        cls._include_private_ipv6_access)

  def Run(self, args):
    """Issues requests necessary to update Subnetworks."""
    holder = base_classes.ComputeApiHolder(self.ReleaseTrack())
    client = holder.client
    subnet_ref = self.SUBNETWORK_ARG.ResolveAsResource(args, holder.resources)

    aggregation_interval = None
    flow_sampling = None
    metadata = None

    if self._include_alpha_logging:
      aggregation_interval = args.aggregation_interval
      flow_sampling = args.flow_sampling
      metadata = args.metadata
    elif self._include_beta_logging:
      aggregation_interval = args.logging_aggregation_interval
      flow_sampling = args.logging_flow_sampling
      metadata = args.logging_metadata

    set_role_active = None
    drain_timeout_seconds = None
    if self._include_l7_internal_load_balancing:
      drain_timeout_seconds = args.drain_timeout
      if args.role is not None:
        set_role_active = getattr(args, 'role', None) == 'ACTIVE'

    enable_private_ipv6_access = None
    private_ipv6_google_access_type = None
    private_ipv6_google_access_service_accounts = None
    if self._include_private_ipv6_access:
      enable_private_ipv6_access = args.enable_private_ipv6_access
      private_ipv6_google_access_type = args.private_ipv6_google_access_type
      private_ipv6_google_access_service_accounts = (
          args.private_ipv6_google_access_service_accounts)

    return subnets_utils.MakeSubnetworkUpdateRequest(
        client,
        subnet_ref,
        self._include_alpha_logging,
        self._include_beta_logging,
        enable_private_ip_google_access=args.enable_private_ip_google_access,
        add_secondary_ranges=args.add_secondary_ranges,
        remove_secondary_ranges=args.remove_secondary_ranges,
        aggregation_interval=aggregation_interval,
        flow_sampling=flow_sampling,
        metadata=metadata,
        enable_flow_logs=args.enable_flow_logs,
        set_role_active=set_role_active,
        drain_timeout_seconds=drain_timeout_seconds,
        enable_private_ipv6_access=enable_private_ipv6_access,
        private_ipv6_google_access_type=private_ipv6_google_access_type,
        private_ipv6_google_access_service_accounts=(
            private_ipv6_google_access_service_accounts))


@base.ReleaseTracks(base.ReleaseTrack.BETA)
class UpdateBeta(Update):
  """Updates properties of an existing Google Compute Engine subnetwork."""

  _include_beta_logging = True
  _include_l7_internal_load_balancing = True


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class UpdateAlpha(UpdateBeta):
  """Updates properties of an existing Google Compute Engine subnetwork."""

  _include_alpha_logging = True
  _include_beta_logging = False
  _include_private_ipv6_access = True
