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
"""Implements command to update a given guest policy."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.compute.os_config import osconfig_utils
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.compute.os_config import resource_args


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class Update(base.Command):
  r"""Update the given guest policy for a project, a folder, or an organization.

  ## EXAMPLES

    To update the description of guest policy 'policy1' in the project
    'project1', run:

          $ {command} policy1 \
          --description='new description' --project=project1

    To update the guest policy 'policy1' in the project 'project1', run:

          $ {command} policy1 \
          --file=path_to_config_file --project=project1

    To update the guest policy 'policy1' in the organization '12345', run:

          $ {command} policy1 \
          --file=path_to_config_file --organization=12345

  """

  @staticmethod
  def Args(parser):
    resource_args.AddGuestPolicyResourceArg(parser, 'to update.')
    update_group = parser.add_group(help='The update component.', required=True)
    update_group.add_argument(
        '--file',
        help="""\
        The JSON or YAML file with the updated guest policy.

        If this file specifies an "etag" value, the update will succeed only if
        the policy already in place matches that etag. A policy in a file that
        does not contain an etag value will simply replace the existing policy.
        """)
    update_group.add_argument(
        '--description',
        type=str,
        help="""\
        Description of the guest policy to update. Length of the description is
        limited to 1024 characters.

        If specified, it will override any description provided in the file.""")
    parser.add_argument(
        '--etag',
        type=str,
        help="""\
        The etag value of the guest policy to update.

        If specified, it will override any etag provided in the file, and the
        update will succeed only if the policy already in place matches this
        etag.""")

  def Run(self, args):
    guest_policy_ref = args.CONCEPTS.guest_policy.Parse()
    guest_policy_type = guest_policy_ref.type_
    guest_policy_name = guest_policy_ref.result.RelativeName()

    release_track = self.ReleaseTrack()
    client = osconfig_utils.GetClientInstance(release_track)
    messages = osconfig_utils.GetClientMessages(release_track)

    update_fields = []
    if args.file:
      (guest_policy,
       update_fields) = osconfig_utils.GetResourceAndUpdateFieldsFromFile(
           args.file, messages.GuestPolicy)
    else:
      guest_policy = messages.GuestPolicy()

    if args.description:
      guest_policy.description = args.description
      update_fields.append('description')
    if args.etag:
      guest_policy.etag = args.etag
      update_fields.append('etag')
    update_mask = ','.join(sorted(list(set(update_fields))))

    if args.organization or guest_policy_type == type(
        guest_policy_type).organization_guest_policy:
      request = messages.OsconfigOrganizationsGuestPoliciesPatchRequest(
          guestPolicy=guest_policy,
          name=guest_policy_name,
          updateMask=update_mask)
      service = client.organizations_guestPolicies
    elif args.folder or guest_policy_type == type(
        guest_policy_type).folder_guest_policy:
      request = messages.OsconfigFoldersGuestPoliciesPatchRequest(
          guestPolicy=guest_policy,
          name=guest_policy_name,
          updateMask=update_mask)
      service = client.folders_guestPolicies
    else:
      request = messages.OsconfigProjectsGuestPoliciesPatchRequest(
          guestPolicy=guest_policy,
          name=guest_policy_name,
          updateMask=update_mask)
      service = client.projects_guestPolicies

    return service.Patch(request)
