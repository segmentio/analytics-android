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
"""Utility functions for managing GCE OS Configs."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from apitools.base.py import encoding
from enum import Enum
from googlecloudsdk.api_lib.util import apis
from googlecloudsdk.api_lib.util import waiter
from googlecloudsdk.calliope import base
from googlecloudsdk.calliope import exceptions
from googlecloudsdk.core import yaml


class InstanceDetailsStates(Enum):
  """Indicate instance progress during a patch job execution."""
  NOTIFIED = 1
  PATCHING = 2
  FINISHED = 3


INSTANCE_DETAILS_KEY_MAP = {
    'instancesAcked': InstanceDetailsStates.NOTIFIED,
    'instancesApplyingPatches': InstanceDetailsStates.PATCHING,
    'instancesDownloadingPatches': InstanceDetailsStates.PATCHING,
    'instancesFailed': InstanceDetailsStates.FINISHED,
    'instancesInactive': InstanceDetailsStates.FINISHED,
    'instancesNotified': InstanceDetailsStates.NOTIFIED,
    'instancesPending': InstanceDetailsStates.NOTIFIED,
    'instancesRebooting': InstanceDetailsStates.PATCHING,
    'instancesStarted': InstanceDetailsStates.PATCHING,
    'instancesSucceeded': InstanceDetailsStates.FINISHED,
    'instancesSucceededRebootRequired': InstanceDetailsStates.FINISHED,
    'instancesTimedOut': InstanceDetailsStates.FINISHED,
}

_API_CLIENT_NAME = 'osconfig'
_API_CLIENT_VERSION_MAP = {base.ReleaseTrack.ALPHA: 'v1alpha2'}


def GetParentUriPath(parent_name, parent_id):
  """Return the URI path of a GCP parent resource."""
  return '/'.join([parent_name, parent_id])


def GetProjectUriPath(project):
  """Return the URI path of a GCP project."""
  return GetParentUriPath('projects', project)


def GetFolderUriPath(folder):
  """Return the URI path of a GCP folder."""
  return GetParentUriPath('folders', folder)


def GetOrganizationUriPath(organization):
  """Return the URI path of a GCP organization."""
  return GetParentUriPath('organizations', organization)


def GetPatchJobUriPath(project, patch_job):
  """Return the URI path of an osconfig patch job."""
  return '/'.join(['projects', project, 'patchJobs', patch_job])


def GetPatchJobName(patch_job_uri):
  """Return the name of a patch job from its URI."""
  return patch_job_uri.split('/')[3]


def GetGuestPolicyRelativePath(parent, guest_policy):
  """Return the relative path of an osconfig guest policy."""
  return '/'.join([parent, 'guestPolicies', guest_policy])


def GetClientClass(release_track, api_version_override=None):
  return apis.GetClientClass(
      _API_CLIENT_NAME, api_version_override or
      _API_CLIENT_VERSION_MAP[release_track])


def GetClientInstance(release_track, api_version_override=None):
  return apis.GetClientInstance(
      _API_CLIENT_NAME, api_version_override or
      _API_CLIENT_VERSION_MAP[release_track])


def GetClientMessages(release_track, api_version_override=None):
  return apis.GetMessagesModule(
      _API_CLIENT_NAME, api_version_override or
      _API_CLIENT_VERSION_MAP[release_track])


class Poller(waiter.OperationPoller):
  """Poller for synchronous patch job execution."""

  def __init__(self, client, messages):
    """Initializes poller for patch job execution.

    Args:
      client: API client of the OsConfig service.
      messages: API messages of the OsConfig service.
    """
    self.client = client
    self.messages = messages
    self.patch_job_terminal_states = [
        self.messages.PatchJob.StateValueValuesEnum.SUCCEEDED,
        self.messages.PatchJob.StateValueValuesEnum.COMPLETED_WITH_ERRORS,
        self.messages.PatchJob.StateValueValuesEnum.TIMED_OUT,
        self.messages.PatchJob.StateValueValuesEnum.CANCELED
    ]

  def IsDone(self, patch_job):
    """Overrides."""
    return patch_job.state in self.patch_job_terminal_states

  def Poll(self, request):
    """Overrides."""
    return self.client.projects_patchJobs.Get(request)

  def GetResult(self, patch_job):
    """Overrides."""
    return patch_job


def AddFolderAndOrgArgs(parser, noun, verb):
  parent_resource_group = parser.add_group(
      help='The scope of the {}.'.format(noun), mutex=True)
  parent_resource_group.add_argument(
      '--folder', type=str, help='The folder of the {} {}.'.format(noun, verb))
  parent_resource_group.add_argument(
      '--organization',
      type=str,
      help='The organization of the {} {}.'.format(noun, verb))


def GetResourceAndUpdateFieldsFromFile(file_path, resource_message_type):
  try:
    resource_to_parse = yaml.load_path(file_path)
    update_fields = list(resource_to_parse.keys())
    resource = encoding.PyValueToMessage(resource_message_type,
                                         resource_to_parse)
    return (resource, update_fields)
  except (AttributeError) as e:
    raise exceptions.BadFileException(
        'Policy config file [{0}] is not a properly formatted YAML or JSON '
        'file. {1}'.format(file_path, str(e)))
