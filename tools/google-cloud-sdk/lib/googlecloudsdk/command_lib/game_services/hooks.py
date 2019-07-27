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

"""Argument processors for Game Services surface arguments."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import json

from googlecloudsdk.calliope import arg_parsers
from googlecloudsdk.core import exceptions
from googlecloudsdk.core import properties
from googlecloudsdk.core import yaml


PARENT_TEMPLATE = 'projects/{}/locations/{}'
LOCATION_WILDCARD = '-'


class InvalidSpecFileError(exceptions.Error):
  """Error if a spec file is not valid JSON or YAML."""


def FlattenedArgDict(value):
  dict_value = arg_parsers.ArgDict()(value)
  return [{'key': key, 'value': value} for key, value in dict_value.items()]


def ProcessSpecFile(spec_file):
  """Reads a JSON/YAML spec_file and returns JSON format of it."""

  try:
    spec = json.loads(spec_file)
  except ValueError as e:
    try:
      spec = yaml.load(spec_file)
    except yaml.YAMLParseError as e:
      raise InvalidSpecFileError('Error parsing spec file: [{}]'.format(e))
  return json.dumps(spec)


def AddDefaultLocationToListRequest(ref, args, req):
  """Python hook for yaml commands to wildcard the location in list requests."""
  del ref
  project = properties.VALUES.core.project.Get(required=True)
  location = args.location or LOCATION_WILDCARD
  req.parent = PARENT_TEMPLATE.format(project, location)
  return req
