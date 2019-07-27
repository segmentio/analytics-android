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
"""Utility for interacting with Cloud Build Artifacts requests."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import os
import re

from googlecloudsdk.api_lib import build_artifacts
from googlecloudsdk.api_lib.build_artifacts import exceptions as cba_exceptions
from googlecloudsdk.core import log
from googlecloudsdk.core import properties

from six.moves.urllib import parse

_INVALID_REPO_NAME_ERROR = (
    "Names may only contain lowercase letters, numbers, and hyphens, and must "
    "begin with a letter and end with a letter or number.")

_VALID_LOCATIONS = [
    "us-central1", "us-east1", "europe-west1", "europe-west2", "asia-east2",
    "asia-northeast1", "us"
]

_REPO_REGEX = "^[a-z]([a-z0-9-]*[a-z0-9])?$"


def _GetMessagesForResource(resource_ref):
  api_version = resource_ref.GetCollectionInfo().api_version
  messages = build_artifacts.Messages(api_version)
  return messages


def _IsValidLocation(location):
  return location in _VALID_LOCATIONS


def _IsValidRepoName(repo_name):
  return re.match(_REPO_REGEX, repo_name) is not None


def GetProject(args):
  """Gets project resource from either argument flag or attribute."""
  return args.project or properties.VALUES.core.project.GetOrFail()


def GetRepo(args):
  """Gets repository resource from either argument flag or attribute."""
  return (args.repository or
          properties.VALUES.build_artifacts.repository.GetOrFail())


def AppendRepoDataToRequest(repo_ref, repo_args, request):
  """Adds repository data to CreateRepositoryRequest."""
  if not _IsValidRepoName(repo_ref.repositoriesId):
    raise cba_exceptions.InvalidInputValueError(_INVALID_REPO_NAME_ERROR)
  if not _IsValidLocation(repo_args.location):
    raise cba_exceptions.UnsupportedLocationError(
        "{} is not a valid location. Valid locations are [{}].".format(
            repo_args.location, ", ".join(_VALID_LOCATIONS)))
  messages = _GetMessagesForResource(repo_ref)
  repo = messages.Repository(
      name=repo_ref.RelativeName(),
      description=repo_args.description,
      format=messages.Repository.FormatValueValuesEnum(
          repo_args.repository_format.upper()),
      location=repo_args.location)
  request.repository = repo
  request.repositoryId = repo_ref.repositoriesId

  return request


def URLEscapePackageName(pkg_ref, unused_args, request):
  """URL escapes package name for ListVersionsRequest."""
  request.parent = "{}/packages/{}".format(
      pkg_ref.Parent().RelativeName(),
      parse.quote(pkg_ref.packagesId, safe="@:"))
  return request


def URLDecodePackageName(response, unused_args):
  """URL decodes package name for ListPackagesResponse."""
  ret = []
  for ver in response:
    ver.name = parse.unquote(os.path.basename(ver.name))
    ret.append(ver)
  return ret


def AppendParentInfoToListReposResponse(response, args):
  """Adds log to clarify parent resources for ListRepositoriesRequest."""
  if response:
    log.status.Print("Listing items under project {}.\n".format(
        GetProject(args)))
  return response


def AppendParentInfoToListPackagesResponse(response, args):
  """Adds log to clarify parent resources for ListPackagesRequest."""
  if response:
    log.status.Print("Listing items under project {}, repository {}.\n".format(
        GetProject(args), GetRepo(args)))
  return response


def AppendParentInfoToListVersionsResponse(response, args):
  """Adds log to clarify parent resources for ListVersionsRequest."""
  if response:
    log.status.Print(
        "Listing items under project {}, repository {}, package {}.\n".format(
            GetProject(args), GetRepo(args), args.package))
  return response
