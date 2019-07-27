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
"""Implements command to list ongoing and completed patch jobs."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from apitools.base.py import list_pager
from googlecloudsdk.api_lib.compute.os_config import osconfig_utils
from googlecloudsdk.calliope import base
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources


_DEFAULT_LIMIT = 10


def _TransformPatchJobDescription(resource):
  max_len = 30  # Show only the first 30 characters if description is long.
  description = resource.get('description', '')
  return (description[:max_len] +
          '..') if len(description) > max_len else description


def _TransformNumInstances(resource):
  """Sum up number of instances in a patch job."""
  if 'instanceDetailsSummary' not in resource:
    return None

  instance_details_summary = resource['instanceDetailsSummary']
  num_instances = 0
  for status in osconfig_utils.INSTANCE_DETAILS_KEY_MAP.keys():
    num_instances += int(instance_details_summary.get(status, 0))
  return num_instances


def _MakeGetUriFunc():
  """Return a transformation function from a patch job resource to an URI."""

  def UriFunc(resource):
    ref = resources.REGISTRY.Parse(
        resource.name,
        params={
            'projects': properties.VALUES.core.project.GetOrFail,
            'patchJobs': resource.name
        },
        collection='osconfig.projects.patchJobs')
    return ref.SelfLink()

  return UriFunc


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class List(base.ListCommand):
  """List ongoing and completed patch jobs.

  ## EXAMPLES

  To list patch jobs in the current project, run:

        $ {command}

  """

  @staticmethod
  def Args(parser):
    base.LIMIT_FLAG.SetDefault(parser, _DEFAULT_LIMIT)
    parser.display_info.AddFormat("""
          table(
            name.basename(),
            description(),
            create_time,
            state,
            num_instances()
          )
        """)
    parser.display_info.AddTransforms({
        'description': _TransformPatchJobDescription,
        'num_instances': _TransformNumInstances
    })
    parser.display_info.AddUriFunc(_MakeGetUriFunc())

  def Run(self, args):
    project = properties.VALUES.core.project.GetOrFail()

    release_track = self.ReleaseTrack()
    client = osconfig_utils.GetClientInstance(
        release_track)
    messages = osconfig_utils.GetClientMessages(
        release_track)

    request = messages.OsconfigProjectsPatchJobsListRequest(
        pageSize=args.page_size,
        parent=osconfig_utils.GetProjectUriPath(project))

    return list_pager.YieldFromList(
        client.projects_patchJobs,
        request,
        limit=args.limit,
        batch_size=args.page_size,
        field='patchJobs',
        batch_size_attribute='pageSize')
