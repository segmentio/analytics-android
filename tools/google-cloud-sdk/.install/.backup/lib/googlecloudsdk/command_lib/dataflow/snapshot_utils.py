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
"""Helpers for writing commands interacting with Cloud Dataflow snapshots.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.dataflow import apis
from googlecloudsdk.calliope import arg_parsers

from googlecloudsdk.core import properties
from googlecloudsdk.core import resources

DATAFLOW_API_DEFAULT_REGION = apis.DATAFLOW_API_DEFAULT_REGION


def ArgsForSnapshotJobRef(parser):
  """Register flags for specifying a single Job ID.

  Args:
    parser: The argparse.ArgParser to configure with job-filtering arguments.
  """
  parser.add_argument(
      '--job-id',
      required=True,
      metavar='JOB_ID',
      help='The job ID to snapshot.')
  parser.add_argument(
      '--region',
      default='us-central1',
      metavar='REGION_ID',
      help='The region ID of the snapshot and job\'s regional endpoint.')


def ArgsForSnapshotTtl(parser):
  """Register flags for specifying a snapshot ttl.

  Args:
    parser: the argparse.ArgParser to configure with a ttl argument.
  """
  parser.add_argument(
      '--snapshot-ttl',
      default='7d',
      metavar='DURATION',
      type=arg_parsers.Duration(lower_bound='1s', upper_bound='7d'),
      help='Time to live for the snapshot.')


def ExtractSnapshotJobRef(args):
  """Extract the Job Ref for a command. Used with ArgsForSnapshotJobRef.

  Args:
    args: The command line arguments.
  Returns:
    A Job resource.
  """
  job = args.job_id
  region = args.region or DATAFLOW_API_DEFAULT_REGION
  return resources.REGISTRY.Parse(
      job,
      params={
          'projectId': properties.VALUES.core.project.GetOrFail,
          'location': region
      },
      collection='dataflow.projects.locations.jobs')


def ExtractSnapshotTtlDuration(args):
  """Extract the Duration string for the Snapshot ttl.

  Args:
    args: The command line arguments.
  Returns:
    A duration string for the snapshot ttl.
  """
  return str(args.snapshot_ttl) + 's'
