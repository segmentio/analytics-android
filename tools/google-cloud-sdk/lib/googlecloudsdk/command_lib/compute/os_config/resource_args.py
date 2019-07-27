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
"""Shared resource flags for OS Config commands."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.calliope.concepts import concepts
from googlecloudsdk.calliope.concepts import deps
from googlecloudsdk.calliope.concepts import multitype
from googlecloudsdk.command_lib.util.concepts import concept_parsers
from googlecloudsdk.command_lib.util.concepts import presentation_specs


def PatchJobAttributeConfig():
  return concepts.ResourceParameterAttributeConfig(
      name='patch_job', help_text='An OS patch job.')


def GetPatchJobResourceSpec():
  return concepts.ResourceSpec(
      'osconfig.projects.patchJobs',
      resource_name='patch_job',
      projectsId=concepts.DEFAULT_PROJECT_ATTRIBUTE_CONFIG,
      patchJobsId=PatchJobAttributeConfig())


def CreatePatchJobResourceArg(verb, plural=False):
  """Create a resource argument for a OS Config patch job.

  Args:
    verb: str, The verb to describe the resource, such as 'to describe'.
    plural: bool, If True, use a resource argument that returns a list.

  Returns:
    PresentationSpec for the resource argument.
  """
  noun = 'Patch job' + ('s' if plural else '')
  return presentation_specs.ResourcePresentationSpec(
      'patch_job',
      GetPatchJobResourceSpec(),
      '{} {}'.format(noun, verb),
      required=True,
      plural=plural,
      prefixes=False)


def AddPatchJobResourceArg(parser, verb, plural=False):
  """Create a resource argument for a OS Config patch job.

  Args:
    parser: The parser for the command.
    verb: str, The verb to describe the resource, such as 'to describe'.
    plural: bool, If True, use a resource argument that returns a list.
  """
  concept_parsers.ConceptParser([CreatePatchJobResourceArg(
      verb, plural)]).AddToParser(parser)


def GuestPolicyAttributeConfig():
  return concepts.ResourceParameterAttributeConfig(
      name='guest_policy', help_text='A guest policy.')


def ProjectAttributeConfig():
  """Create a project attribute config.

  This is identical to the default project attribute config but without a
  property fallthrough. This way the help text about reading the project ID from
  core/project will not show.

  Returns:
    A project attribute config.
  """
  return concepts.ResourceParameterAttributeConfig(
      name='project',
      help_text='The Cloud project for the {resource}.',
      fallthroughs=[deps.ArgFallthrough('--project')])


def FolderAttributeConfig():
  return concepts.ResourceParameterAttributeConfig(
      name='folder', help_text='The Cloud folder of the {resource}.')


def OrganizationAttributeConfig():
  return concepts.ResourceParameterAttributeConfig(
      name='organization',
      help_text='The Cloud organization of the {resource}.')


def GetProjectGuestPolicyResourceSpec():
  return concepts.ResourceSpec(
      'osconfig.projects.guestPolicies',
      resource_name='project_guest_policy',
      projectsId=ProjectAttributeConfig(),
      guestPoliciesId=GuestPolicyAttributeConfig())


def GetFolderGuestPolicyResourceSpec():
  return concepts.ResourceSpec(
      'osconfig.folders.guestPolicies',
      resource_name='folder_guest_policy',
      foldersId=FolderAttributeConfig(),
      guestPoliciesId=GuestPolicyAttributeConfig())


def GetOrganizationGuestPolicyResourceSpec():
  return concepts.ResourceSpec(
      'osconfig.organizations.guestPolicies',
      resource_name='organization_guest_policy',
      organizationsId=OrganizationAttributeConfig(),
      guestPoliciesId=GuestPolicyAttributeConfig())


def GetGuestPolicyResourceSpec():
  return multitype.MultitypeResourceSpec(
      'guest_policy', GetProjectGuestPolicyResourceSpec(),
      GetFolderGuestPolicyResourceSpec(),
      GetOrganizationGuestPolicyResourceSpec())


def CreateGuestPolicyResourceArg(verb, plural=False):
  """Create a resource argument for a OS Config guest policy.

  Args:
    verb: str, The verb to describe the resource, such as 'to describe'.
    plural: bool, If True, use a resource argument that returns a list.

  Returns:
    PresentationSpec for the resource argument.
  """
  noun = 'Guest poli' + ('cies' if plural else 'cy')
  return presentation_specs.MultitypeResourcePresentationSpec(
      'guest_policy',
      GetGuestPolicyResourceSpec(),
      '{} {}'.format(noun, verb),
      required=True,
      plural=plural,
      prefixes=False)


def AddGuestPolicyResourceArg(parser, verb, plural=False):
  """Create a resource argument for a OS Config guest policy.

  Args:
    parser: The parser for the command.
    verb: str, The verb to describe the resource, such as 'to describe'.
    plural: bool, If True, use a resource argument that returns a list.
  """
  concept_parsers.ConceptParser([CreateGuestPolicyResourceArg(verb, plural)
                                ]).AddToParser(parser)
