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
"""Shared resource arguments and flags."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.calliope import arg_parsers
from googlecloudsdk.calliope.concepts import concepts
from googlecloudsdk.command_lib.util.concepts import concept_parsers
from googlecloudsdk.core import resources

# Args


def AddCreateIfMissing(parser, resource, positional=False, **kwargs):
  parser.add_argument(
      _ArgOrFlag('create-if-missing', positional),
      action='store_true',
      help=('Create the {resource} if it does not exist. If this flag is not '
            'set, the command will return an error when attempting to update a '
            '{resource} that does not exist.').format(resource=resource),
      **kwargs)


def AddDataFile(parser, positional=False, **kwargs):
  parser.add_argument(
      _ArgOrFlag('data-file', positional),
      metavar='PATH',
      help=('File path from which to read secret data. Set this to "-" to read '
            'the secret data from stdin.'),
      **kwargs)


def AddProject(parser, positional=False, **kwargs):
  concept_parsers.ConceptParser.ForResource(
      name=_ArgOrFlag('project', positional),
      resource_spec=GetProjectResourceSpec(),
      group_help='The project ID.',
      **kwargs).AddToParser(parser)


# TODO(b/135570696): may want to convert to resource arg & add fallthrough
def AddLocations(parser, resource, positional=False, **kwargs):
  parser.add_argument(
      _ArgOrFlag('locations', positional),
      action=arg_parsers.UpdateAction,
      metavar='LOCATION',
      type=arg_parsers.ArgList(),
      help=('Comma-separated list of locations in which the {resource} should '
            'be replicated.').format(resource=resource),
      **kwargs)


def AddSecret(parser, purpose, positional=False, **kwargs):
  concept_parsers.ConceptParser.ForResource(
      name=_ArgOrFlag('secret', positional),
      resource_spec=GetSecretResourceSpec(),
      group_help='The secret {}.'.format(purpose),
      **kwargs).AddToParser(parser)


def AddVersion(parser, purpose, positional=False, **kwargs):
  concept_parsers.ConceptParser.ForResource(
      name=_ArgOrFlag('version', positional),
      resource_spec=GetVersionResourceSpec(),
      group_help='Numeric secret version {}.'.format(purpose),
      **kwargs).AddToParser(parser)


def _ArgOrFlag(name, positional):
  """Returns the argument name in resource argument format or flag format.

  Args:
      name (str): name of the argument
      positional (bool): whether the argument is positional

  Returns:
      arg (str): the argument or flag
  """
  if positional:
    return name.upper().replace('-', '_')
  return '--{}'.format(name)


### Attribute configurations


def GetProjectAttributeConfig():
  return concepts.DEFAULT_PROJECT_ATTRIBUTE_CONFIG


def GetLocationAttributeConfig():
  return concepts.ResourceParameterAttributeConfig(
      name='location',
      help_text='The location of the {resource}.',
      completion_request_params={'fieldMask': 'name'},
      completion_id_field='name')


def GetSecretAttributeConfig():
  return concepts.ResourceParameterAttributeConfig(
      name='secret',
      help_text='The secret of the {resource}.',
      completion_request_params={'fieldMask': 'name'},
      completion_id_field='name')


def GetVersionAttributeConfig():
  return concepts.ResourceParameterAttributeConfig(
      name='version',
      help_text='The version of the {resource}.',
      completion_request_params={'fieldMask': 'name'},
      completion_id_field='name')


# Resource specs


def GetProjectResourceSpec():
  return concepts.ResourceSpec(
      resource_collection='secretmanager.projects',
      resource_name='project',
      plural_name='projects',
      disable_auto_completers=False,
      projectsId=GetProjectAttributeConfig())


def GetLocationResourceSpec():
  return concepts.ResourceSpec(
      resource_collection='secretmanager.projects.locations',
      resource_name='location',
      plural_name='locations',
      disable_auto_completers=False,
      locationsId=GetLocationAttributeConfig(),
      projectsId=GetProjectAttributeConfig())


def GetSecretResourceSpec():
  return concepts.ResourceSpec(
      resource_collection='secretmanager.projects.secrets',
      resource_name='secret',
      plural_name='secrets',
      disable_auto_completers=False,
      secretsId=GetSecretAttributeConfig(),
      projectsId=GetProjectAttributeConfig())


def GetVersionResourceSpec():
  return concepts.ResourceSpec(
      'secretmanager.projects.secrets.versions',
      resource_name='version',
      plural_name='version',
      disable_auto_completers=False,
      versionsId=GetVersionAttributeConfig(),
      secretsId=GetSecretAttributeConfig(),
      projectsId=GetProjectAttributeConfig())


# Resource parsers


def ParseProjectRef(ref, **kwargs):
  kwargs['collection'] = 'secretmanager.projects'
  return resources.REGISTRY.Parse(ref, **kwargs)


def ParseLocationRef(ref, **kwargs):
  kwargs['collection'] = 'secretmanager.projects.locations'
  return resources.REGISTRY.Parse(ref, **kwargs)


def ParseSecretRef(ref, **kwargs):
  kwargs['collection'] = 'secretmanager.projects.secrets'
  return resources.REGISTRY.Parse(ref, **kwargs)


def ParseVersionRef(ref, **kwargs):
  kwargs['collection'] = 'secretmanager.projects.secrets.versions'
  return resources.REGISTRY.Parse(ref, **kwargs)
