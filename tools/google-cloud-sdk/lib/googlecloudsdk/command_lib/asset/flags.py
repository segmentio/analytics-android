# -*- coding: utf-8 -*- #
# Copyright 2018 Google LLC. All Rights Reserved.
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
"""Flags for commands in cloudasset."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.calliope import arg_parsers


def AddOrganizationArgs(parser):
  parser.add_argument(
      '--organization',
      metavar='ORGANIZATION_ID',
      help='The ID of the organization which is the root asset.')


def AddFolderArgs(parser):
  parser.add_argument(
      '--folder',
      metavar='FOLDER_ID',
      help='The ID of the folder which is the root asset.')


def AddParentArgs(parser):
  parent_group = parser.add_group(mutex=True)
  AddOrganizationArgs(parent_group)
  AddFolderArgs(parent_group)


def AddSnapshotTimeArgs(parser):
  parser.add_argument(
      '--snapshot-time',
      type=arg_parsers.Datetime.Parse,
      help=('Timestamp to take a snapshot on assets. This can only be a '
            'current or past time. If not specified, the current time will be '
            'used. Due to delays in resource data collection and indexing, '
            'there is a volatile window during which running the same query at '
            'different times may return different results. '
            'See $ gcloud topic datetimes for information on time formats.'))


def AddAssetTypesArgs(parser):
  parser.add_argument(
      '--asset-types',
      metavar='ASSET_TYPES',
      type=arg_parsers.ArgList(),
      default=[],
      help=(
          'A list of asset types (i.e., "compute.googleapis.com/Disk") to take '
          'a snapshot. If specified and non-empty, only assets matching the '
          'specified types will be returned. '
          'See https://cloud.google.com/resource-manager/docs/'
          'cloud-asset-inventory/overview '
          'for supported asset types.'))


def AddContentTypeArgs(parser, required):
  """--content-type argument for asset export and get-history."""
  if required:
    help_text = (
        'Asset content type. Choices are `resource`, `iam-policy` and '
        '`org-policy`. Specifying `resource` will export resource metadata, '
        'specifying `iam-policy` will export IAM policy set on assets, '
        'specifying `org-policy` will export Org Policy set on asset, and '
        'specifying `access-policy` will export Access Policy set on asset.')
  else:
    help_text = (
        'Asset content type. If specified, only content matching the '
        'specified type will be returned. Otherwise, no content but the '
        'asset name will be returned. Choices are `resource`, '
        '`iam-policy`. Specifying `resource` will export resource '
        'metadata, and specifying `iam-policy` will export IAM policy set '
        'on assets.')

  parser.add_argument(
      '--content-type',
      required=required,
      choices=['resource', 'iam-policy', 'org-policy', 'access-policy'],
      help=help_text)


def AddOutputPathArgs(parser, required):
  parser.add_argument(
      '--output-path',
      metavar='OUTPUT_PATH',
      required=required,
      type=arg_parsers.RegexpValidator(
          r'^gs://.*',
          '--output-path must be a Google Cloud Storage URI starting with '
          '"gs://". For example, "gs://bucket_name/object_name"'),
      help='Google Cloud Storage URI where the results will go. '
      'URI must start with "gs://". For example, "gs://bucket_name/object_name"'
  )


def AddOutputPathPrefixArgs(parser):
  parser.add_argument(
      '--output-path-prefix',
      type=arg_parsers.RegexpValidator(
          r'^gs://.*/.*',
          '--output-path-prefix must be a Google Cloud Storage URI starting '
          'with "gs://". For example, "gs://bucket_name/object_name_prefix"'),
      help=(
          'Google Cloud Storage URI where the results will go. '
          'URI must start with "gs://". For example, '
          '"gs://bucket_name/object_name_prefix", in which case each exported '
          'object uri is in format: '
          '"gs://bucket_name/object_name_prefix/<asset type>/<shard number>" '
          'and it only contains assets for that type.'))


def AddDestinationArgs(parser):
  destination_group = parser.add_group(
      mutex=True,
      required=True,
      help='The destination path for exporting assets.')
  AddOutputPathArgs(destination_group, required=False)
  AddOutputPathPrefixArgs(destination_group)


def AddAssetNamesArgs(parser):
  parser.add_argument(
      '--asset-names',
      metavar='ASSET_NAMES',
      required=True,
      type=arg_parsers.ArgList(),
      help=
      ('A list of full names of the assets to get the history for. See '
       'https://cloud.google.com/apis/design/resource_names#full_resource_name '
       'for name format.'))


def AddStartTimeArgs(parser):
  parser.add_argument(
      '--start-time',
      required=True,
      type=arg_parsers.Datetime.Parse,
      help=('Start time of the time window (inclusive) for the asset history. '
            'Must be later than 2018-10-02T00:00:00Z. '
            'See $ gcloud topic datetimes for information on time formats.'))


def AddEndTimeArgs(parser):
  parser.add_argument(
      '--end-time',
      required=False,
      type=arg_parsers.Datetime.Parse,
      help=('End time of the time window (exclusive) for the asset history. '
            'Defaults to current time if not specified. '
            'See $ gcloud topic datetimes for information on time formats.'))


def AddOperationArgs(parser):
  parser.add_argument(
      'id',
      metavar='OPERATION_NAME',
      help='Name of the operation to describe.')
