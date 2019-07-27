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
"""Implements command to list guest policies."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from apitools.base.py import list_pager
from googlecloudsdk.api_lib.compute.os_config import osconfig_utils
from googlecloudsdk.calliope import base
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources


def _TransformGuestPolicyDescription(resource):
  max_len = 30  # Show only the first 30 characters if description is long.
  description = resource.get('description', '')
  return (description[:max_len] +
          '..') if len(description) > max_len else description


def _MakeGetUriFunc():
  """Return a transformation function from a guest policy resource to an URI."""

  def UriFunc(resource):
    parent_type = resource.name.split('/')[0]
    ref = resources.REGISTRY.Parse(
        resource.name,
        collection='osconfig.{}.guestPolicies'.format(parent_type))
    return ref.SelfLink()

  return UriFunc


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class List(base.ListCommand):
  """List guest policies in a project, a folder, or an organization.

  ## EXAMPLES

    To list guest policies in the current project, run:

          $ {command}

    To list guest policies in the organization '12345', run:

          $ {command} --organization=12345

  """

  @staticmethod
  def Args(parser):
    osconfig_utils.AddFolderAndOrgArgs(parser, 'guest policies', 'to list')
    parser.display_info.AddFormat("""
          table(
            name.basename(),
            description(),
            create_time,
            update_time
          )
        """)
    parser.display_info.AddTransforms(
        {'description': _TransformGuestPolicyDescription})
    parser.display_info.AddUriFunc(_MakeGetUriFunc())

  def Run(self, args):
    release_track = self.ReleaseTrack()
    client = osconfig_utils.GetClientInstance(release_track)
    messages = osconfig_utils.GetClientMessages(release_track)

    if args.organization:
      request = messages.OsconfigOrganizationsGuestPoliciesListRequest(
          pageSize=args.page_size,
          parent=osconfig_utils.GetOrganizationUriPath(args.organization))
      service = client.organizations_guestPolicies
    elif args.folder:
      request = messages.OsconfigFoldersGuestPoliciesListRequest(
          pageSize=args.page_size,
          parent=osconfig_utils.GetFolderUriPath(args.folder))
      service = client.folders_guestPolicies
    else:
      project = properties.VALUES.core.project.GetOrFail()
      request = messages.OsconfigProjectsGuestPoliciesListRequest(
          pageSize=args.page_size,
          parent=osconfig_utils.GetProjectUriPath(project))
      service = client.projects_guestPolicies

    return list_pager.YieldFromList(
        service,
        request,
        limit=args.limit,
        batch_size=args.page_size,
        field='guestPolicies',
        batch_size_attribute='pageSize')
