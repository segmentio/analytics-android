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
"""Implements command to create a new guest policy."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.compute.os_config import osconfig_utils
from googlecloudsdk.calliope import base
from googlecloudsdk.core import properties


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class Create(base.Command):
  r"""Create a guest policy for a project, a folder, or an organization.

  ## EXAMPLES

    To create a guest policy 'policy1' in the current project, run:

          $ {command} policy1 --file=path_to_config_file

    To create a guest policy 'policy1' in the organization '12345', run:

          $ {command} policy1 --file=path_to_config_file \
          --organization=12345

  """

  @staticmethod
  def Args(parser):
    parser.add_argument(
        'POLICY_ID',
        type=str,
        help="""\
        ID of the guest policy to create.

        This ID must contain only lowercase letters, numbers, and hyphens, start
        with a letter, end with a number or a letter, be between 1-63
        characters, and unique within the project, folder, or organization.""")
    parser.add_argument(
        '--file',
        required=True,
        help='The JSON or YAML file with the guest policy to create.')
    parser.add_argument(
        '--description',
        type=str,
        help="""\
        Description of the guest policy to create. Length of the description is
        limited to 1024 characters.

        If specified, it will override any description provided in the file.""")
    osconfig_utils.AddFolderAndOrgArgs(parser, 'guest policy', 'to create')

  def Run(self, args):
    release_track = self.ReleaseTrack()
    client = osconfig_utils.GetClientInstance(release_track)
    messages = osconfig_utils.GetClientMessages(release_track)

    (guest_policy, _) = osconfig_utils.GetResourceAndUpdateFieldsFromFile(
        args.file, messages.GuestPolicy)

    if args.organization:
      parent_path = osconfig_utils.GetOrganizationUriPath(args.organization)
      request = messages.OsconfigOrganizationsGuestPoliciesCreateRequest(
          guestPolicy=guest_policy,
          guestPolicyId=args.POLICY_ID,
          parent=parent_path)
      service = client.organizations_guestPolicies
    elif args.folder:
      parent_path = osconfig_utils.GetFolderUriPath(args.folder)
      request = messages.OsconfigFoldersGuestPoliciesCreateRequest(
          guestPolicy=guest_policy,
          guestPolicyId=args.POLICY_ID,
          parent=parent_path)
      service = client.folders_guestPolicies
    else:
      project = properties.VALUES.core.project.GetOrFail()
      parent_path = osconfig_utils.GetProjectUriPath(project)
      request = messages.OsconfigProjectsGuestPoliciesCreateRequest(
          guestPolicy=guest_policy,
          guestPolicyId=args.POLICY_ID,
          parent=parent_path)
      service = client.projects_guestPolicies

    return service.Create(request)
