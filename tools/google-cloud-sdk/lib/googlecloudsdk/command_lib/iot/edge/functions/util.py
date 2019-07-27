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
"""Translates Edge function build settings into cloudbuild Build."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import os
import re
import uuid

from apitools.base.py import encoding

from googlecloudsdk.api_lib.cloudbuild import cloudbuild_util
from googlecloudsdk.api_lib.cloudbuild import logs as cb_logs
from googlecloudsdk.api_lib.cloudbuild import snapshot
from googlecloudsdk.api_lib.storage import storage_api

from googlecloudsdk.calliope import exceptions as c_exceptions

from googlecloudsdk.command_lib.cloudbuild import execution

from googlecloudsdk.core import exceptions
from googlecloudsdk.core import execution_utils
from googlecloudsdk.core import log
from googlecloudsdk.core import properties
from googlecloudsdk.core import resources
from googlecloudsdk.core.resource import resource_transform

_VALID_ARCHITECTURES = {
    'x86-64': 'x86_64',
    'armhf': 'armhf',
    'aarch64': 'aarch64'
}
_VALID_FUNCTION_TYPES = {
    'on-demand': 'ondemand',
    'stream-processing': 'streamprocessing'
}

_VALID_GCR_REGIONS = (
    r'(^gcr\.io/)',
    r'(^asia\.gcr\.io/)',
    r'(^eu\.gcr\.io/)',
    r'(^us\.gcr\.io/)',
    )
_GCR_SOURCE_RE = re.compile('|'.join(_VALID_GCR_REGIONS))

_FUNCTION_BUILDER_NAME = 'gcr.io/cloud-iot-edge/function-builder'
_EDGE_VERSION = '0.8.0'


class FunctionBuilderError(exceptions.Error):
  """Error for Edge function build arguments."""


class FailedBuildException(exceptions.Error):
  """Exception for builds that did not succeed."""

  def __init__(self, build):
    super(FailedBuildException,
          self).__init__('build {id} completed with status "{status}"'.format(
              id=build.id, status=build.status))


def BuildEdgeFunctionHook(ref, args, req):
  """Prepares Edge Function Docker image URI from given args.source.

  If the args.source starts with [REGION.]gcr.io, it is considered as
  a prebuilt image in Google Container Registry. Valid values for REGION are
  asia, eu, us. Otherwise, it is considered as local file.

  Args:
    ref: Edge Function resource ref
    args: Arguments from command line
    req: Edge Function create or patch request

  Returns:
    req, with dockerImageUri correctly set to an Edge Function image URI in gcr
  """
  if not args.source or _GCR_SOURCE_RE.match(args.source):
    # function.dockerImageUri is already set, or will keep its original value.
    return req

  if not args.IsSpecified('arch'):
    raise c_exceptions.RequiredArgumentException(
        'arch', 'To build Edge Function from source, please provide target'
        " device's architecture with --arch flag.")
  req.function.dockerImageUri = BuildEdgeFunction(
      ref, args.arch, args.source, req.function)
  return req


def BuildEdgeFunction(function_ref, arch, source, function):
  """Calls Cloudbuild to build Edge Function.

  Args:
    function_ref: Edge function resource
    arch: str, target architecture,
      should be one of 'x86-64', 'armhf', or 'aarch64'
    source: str, GCS URI of source provided from command line
    function: Edge Function message with arguments filled from CLI flags

  Returns:
    str, name of Edge Function docker image, in TAG@sha256:DIGEST format
  """

  function_type = function.functionType.name.lower().replace('_', '-')

  build = _RunBuild(function_ref.functionsId, arch, function_type, source)
  worker_image = build.results.images[0]
  return '{tag}@{digest}'.format(
      tag=worker_image.name, digest=worker_image.digest)


def _ParseSourceUri(gcs_uri):
  """Converts GCS URI to cloudbuild.Source message.

  Args:
    gcs_uri: str, Google Cloud Storage URI in gs://BUCKET/path/to/object
      format

  Returns:
    cloudbuild.Source message filled with parsed URI

  Raises:
    FunctionBuilderError: if gcs_uri is invalid
  """
  messages = cloudbuild_util.GetMessagesModule()

  gcs_uri_re = 'gs://([^/]+)/(.+)'
  match = re.match(gcs_uri_re, gcs_uri)
  if not match:
    raise FunctionBuilderError('the source URI is not a valid GCS URI.')
  return messages.Source(
      storageSource=messages.StorageSource(
          bucket=match.group(1), object=match.group(2)))


def _RunBuild(name, arch, function_type, source):
  """Builds the Edge function image with Cloud Build.

  Args:
    name: str, name of the Edge Function
    arch: str, target architecture,
      should be one of 'x86-64', 'armhf', 'aarch64'
    function_type: str, type of function,
      should be one of 'on-demand', 'stream-processing'
    source: str, GCS URI to source archive object or
      local path to source directory

  Returns:
    Finished cloudbuild.Build message. build.results.images contains
      built image's name and digest

  Raises:
    FailedBuildException: If the build is completed and not 'SUCCESS'
    FunctionBuilderError: For invalid arguments
  """
  client = cloudbuild_util.GetClientInstance()
  messages = cloudbuild_util.GetMessagesModule()

  build_config = _EdgeFunctionBuildMessage(name, arch, function_type, source)

  log.debug('submitting build: ' + repr(build_config))

  # Start the build.
  op = client.projects_builds.Create(
      messages.CloudbuildProjectsBuildsCreateRequest(
          build=build_config, projectId=properties.VALUES.core.project.Get()))
  json = encoding.MessageToJson(op.metadata)
  build = encoding.JsonToMessage(messages.BuildOperationMetadata, json).build

  build_ref = resources.REGISTRY.Create(
      collection='cloudbuild.projects.builds',
      projectId=build.projectId,
      id=build.id)

  log.CreatedResource(build_ref)

  mash_handler = execution.MashHandler(
      execution.GetCancelBuildHandler(client, messages, build_ref))

  # Stream logs from GCS.
  with execution_utils.CtrlCSection(mash_handler):
    build = cb_logs.CloudBuildClient(client, messages).Stream(build_ref)

  if build.status != messages.Build.StatusValueValuesEnum.SUCCESS:
    raise FailedBuildException(build)

  return build


def _EdgeFunctionBuildMessage(name, arch, function_type, source):
  """Returns Build message for Edge function worker.

  Args:
    name: str, the name of the Edge function.
    arch: str, target architecture to build, provided from command line.
      Should be one of 'x86-64', 'armhf', or 'aarch64'
    function_type: str, type of the Edge Function, provided from command line.
      Should be one of 'on-demand' or 'stream-processing'
    source: str, The path to GCS URI for archived source object or path to
      local directory that contains source code for the Edge Function

  Raises:
    FunctionBuilderError: when arguments are not in valid set of choices

  Returns:
    cloudbuild.Build message with Cloud Build configurations
  """
  try:
    arch = _VALID_ARCHITECTURES[arch]
  except KeyError:
    raise FunctionBuilderError('architecture should be one of {}'.format(
        ', '.join(_VALID_ARCHITECTURES.keys())))
  try:
    if function_type not in _VALID_FUNCTION_TYPES.values():
      function_type = _VALID_FUNCTION_TYPES[function_type]
  except KeyError:
    raise FunctionBuilderError('function type should be one of {}'.format(
        ', '.join(_VALID_FUNCTION_TYPES.keys())))

  if source.startswith('gs://'):
    build_source = _ParseSourceUri(source)
  else:
    build_source = _UploadSnapshot(source)

  messages = cloudbuild_util.GetMessagesModule()
  image_name = ('gcr.io/$PROJECT_ID/edge-functions/'
                '{function_type}/{function_name}').format(
                    function_type=function_type, function_name=name)
  # Edge Function builder takes four arguments, in following order:
  # Edge Function image tag, function type(ondemand, streamprocessing),
  # arch(x86_64, armhf, aarch64), version of base image
  build_args = [
      image_name,
      function_type,
      arch,
      _EDGE_VERSION,
  ]
  return messages.Build(
      steps=[messages.BuildStep(name=_FUNCTION_BUILDER_NAME, args=build_args)],
      images=[image_name],
      source=build_source)


def _UploadSnapshot(source):
  """Uploads snapshot of the source directory.

  Args:
    source: str, Path to local directory to be uploaded

  Returns:
    Source message with uploaded source archive.

  Raises:
    BadFileException: if source directory does not exist.
    FunctionBuilderError: if source is invalid.
  """

  if not os.path.exists(source):
    raise c_exceptions.BadFileException(
        'could not find source [{src}]'.format(src=source))

  if os.path.isfile(source):
    raise FunctionBuilderError('cannot use local file for source')

  source_snapshot = snapshot.Snapshot(source)
  size_str = resource_transform.TransformSize(
      source_snapshot.uncompressed_size)
  log.status.Print('Creating temporary tarball archive of {num_files} file(s)'
                   ' totalling {size} before compression.'.format(
                       num_files=len(source_snapshot.files), size=size_str))

  messages = cloudbuild_util.GetMessagesModule()
  gcs_client = storage_api.StorageClient()

  staging_object = _PrepareStagingObject()
  staged_source_obj = source_snapshot.CopyTarballToGCS(
      gcs_client, staging_object)
  return messages.Source(
      storageSource=messages.StorageSource(
          bucket=staged_source_obj.bucket, object=staged_source_obj.name))


def _PrepareStagingObject():
  """Prepares staging object path to be used for source upload."""
  gcs_client = storage_api.StorageClient()
  staging_bucket = '{}_edgefunction'.format(
      properties.VALUES.core.project.Get())
  staging_object = 'source/{}.tgz'.format(uuid.uuid4().hex)

  gcs_client.CreateBucketIfNotExists(staging_bucket)

  return resources.REGISTRY.Create(
      collection='storage.objects',
      bucket=staging_bucket,
      object=staging_object)
