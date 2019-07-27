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
"""Commonly used display formats."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.command_lib.secrets import args as secrets_args

_LOCATION_TABLE = """
table(
  name.basename():label=NAME,
  displayName:label=LOCATION
)
"""

_LOCATION_URI_FUNC = lambda r: secrets_args.ParseLocationRef(r.name).SelfLink()

_SECRET_DATA = """
value(
  payload.data.decode(base64).decode(utf8)
)
"""

_SECRET_TABLE = """
table(
  name.basename():label=NAME,
  policy.replicaLocations.notnull().list():label=LOCATIONS,
  createTime.date():label=CREATED
)
"""

_SECRET_URI_FUNC = lambda r: secrets_args.ParseSecretRef(r.name).SelfLink()

_VERSION_TABLE = """
table(
  name.basename():label=NAME,
  state.enum(secrets.StateVersionJobState).color('destroyed', 'disabled', 'enabled', 'unknown'):label=STATE,
  createTime.date():label=CREATED,
  destroyTime.date(undefined='-'):label=DESTROYED
)
"""

_VERSION_STATE_TRANSFORMS = {
    'secrets.StateVersionJobState::enum': {
        'STATE_UNSPECIFIED': 'unknown',
        'ENABLED': 'enabled',
        'DISABLED': 'disabled',
        'DESTROYED': 'destroyed',
    }
}

_VERSION_URI_FUNC = lambda r: secrets_args.ParseVersionRef(r.name).SelfLink()


def UseLocationTable(parser):
  parser.display_info.AddFormat(_LOCATION_TABLE)
  parser.display_info.AddUriFunc(_LOCATION_URI_FUNC)


def UseSecretTable(parser):
  parser.display_info.AddFormat(_SECRET_TABLE)
  parser.display_info.AddUriFunc(_SECRET_URI_FUNC)


def UseSecretData(parser):
  parser.display_info.AddFormat(_SECRET_DATA)


def UseVersionTable(parser):
  parser.display_info.AddFormat(_VERSION_TABLE)
  parser.display_info.AddTransforms(_VERSION_STATE_TRANSFORMS)
  parser.display_info.AddUriFunc(_VERSION_URI_FUNC)
