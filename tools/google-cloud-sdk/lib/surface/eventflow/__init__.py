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

"""The eventflow command group."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.calliope import base


DETAILED_HELP = {
    'brief': 'Manage your Eventflow resources.',
    'DESCRIPTION': """
        The gcloud eventflow command group lets you manage '
        'your Eventflow resources.
        """,
    'EXAMPLES': """\
        To view your existing triggers, use the `gcloud eventflow triggers list` command:

          $ gcloud eventflow triggers list --cluster <cluster_name> --cluster-location <cluster_location>

        For more information, run:
          $ gcloud eventflow --help
        """
}


@base.Hidden
@base.ReleaseTracks(base.ReleaseTrack.ALPHA)
class Eventflow(base.Group):
  """Manage events."""

  detailed_help = DETAILED_HELP
