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

"""Command to snapshot a Cloud Dataflow job.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.dataflow import apis
from googlecloudsdk.calliope import base
from googlecloudsdk.command_lib.dataflow import snapshot_utils


@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class Create(base.Command):
  """Creates a snapshot for a Cloud Dataflow job.
  """

  @staticmethod
  def Args(parser):
    """Register flags for this command.

    Args:
      parser: argparse.ArgumentParser to register arguments with.
    """
    snapshot_utils.ArgsForSnapshotJobRef(parser)
    snapshot_utils.ArgsForSnapshotTtl(parser)

  def Run(self, args):
    """Runs the command.

    Args:
      args: The arguments that were provided to this command invocation.

    Returns:
      A Snapshot message.
    """
    job_ref = snapshot_utils.ExtractSnapshotJobRef(args)
    return apis.Jobs.Snapshot(
        job_ref.jobId,
        project_id=job_ref.projectId,
        region_id=job_ref.location,
        ttl=snapshot_utils.ExtractSnapshotTtlDuration(args))
