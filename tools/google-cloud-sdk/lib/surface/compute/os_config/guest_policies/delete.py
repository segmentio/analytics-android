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
"""Implements command to delete a given guest policy."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.compute.os_config import osconfig_utils
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.compute.os_config import resource_args
from googlecloudsdk.core import log


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class Delete(base.DeleteCommand):
  """Delete the given guest policy.

  ## EXAMPLES

    To delete the guest policy named 'policy1' in the project 'project1', run:

          $ {command} policy1 --project=project1

    To delete the guest policy named 'policy1' in the organization '12345', run:

          $ {command} policy1 --organization=12345

  """

  @staticmethod
  def Args(parser):
    resource_args.AddGuestPolicyResourceArg(parser, 'to delete.')

  def Run(self, args):
    guest_policy_ref = args.CONCEPTS.guest_policy.Parse()

    release_track = self.ReleaseTrack()
    client = osconfig_utils.GetClientInstance(release_track)
    messages = osconfig_utils.GetClientMessages(release_track)

    guest_policy_type = guest_policy_ref.type_
    guest_policy_name = guest_policy_ref.result.RelativeName()

    if guest_policy_type == type(guest_policy_type).organization_guest_policy:
      request = messages.OsconfigOrganizationsGuestPoliciesDeleteRequest(
          name=guest_policy_name)
      service = client.organizations_guestPolicies
    elif guest_policy_type == type(guest_policy_type).folder_guest_policy:
      request = messages.OsconfigFoldersGuestPoliciesDeleteRequest(
          name=guest_policy_name)
      service = client.folders_guestPolicies
    else:
      request = messages.OsconfigProjectsGuestPoliciesDeleteRequest(
          name=guest_policy_name)
      service = client.projects_guestPolicies

    response = service.Delete(request)
    log.DeletedResource(guest_policy_name)
    return response
