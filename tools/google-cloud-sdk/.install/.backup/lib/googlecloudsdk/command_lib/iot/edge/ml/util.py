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
"""Utilities for Edge ML Model creation."""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from googlecloudsdk.api_lib.cloudbuild import cloudbuild_util
from googlecloudsdk.api_lib.edge import util as edge_util
from googlecloudsdk.api_lib.edgeml import edgeml
from googlecloudsdk.api_lib.edgeml import util as edgeml_util

from googlecloudsdk.calliope import exceptions
from googlecloudsdk.core import exceptions as core_exceptions
from googlecloudsdk.core import log


class InvalidFrameworkException(core_exceptions.InternalError):
  """This error is unexpected. It is here for branch completeness.

  Framework should be one of TFLITE, SCIKIT_LEARN.
  """

  def __init__(self):
    super(InvalidFrameworkException,
          self).__init__('Framework cannot be FRAMEWORK_UNSPECIFIED.')


class UncompilableModelException(core_exceptions.Error):
  """Error for models that attemped to but cannot be optimized for Edge TPU."""

  def __init__(self, reason):
    super(UncompilableModelException, self).__init__(
        'This model cannot be optimized for Edge TPU. {}'.format(str(reason)))


def ParseSamplingInfo(path):
  messages = edge_util.GetMessagesModule()
  sampling_info = cloudbuild_util.LoadMessageFromPath(
      path, messages.MlSamplingInfo, 'Edge ML sampling info')
  return sampling_info


def ProcessModelHook(ref, args, req):
  """Analyzes given model, and converts model if necessary.

  Args:
    ref: A resource ref to the parsed Edge ML Model resource,
      unused in this hook
    args: The parsed args namespace from CLI
    req: Created request for the API call

  Returns:
    req, with new model URI, input/out tensor information, accelerator type
        if applicable.

  Raises:
    InvalidFrameworkException: if framework is FRAMEWORK_UNSPECIFIED.
      This should not happen.
  """
  del ref  # Unused.
  edgeml_messages = edgeml_util.GetMessagesModule()
  model_types = edgeml_messages.AnalyzeModelResponse.ModelTypeValueValuesEnum
  tf_model_types = (
      model_types.TENSORFLOW_LITE,
      model_types.TENSORFLOW_LITE_EDGE_TPU_OPTIMIZED,
      model_types.TENSORFLOW_SAVED_MODEL,
  )

  edge_messages = edge_util.GetMessagesModule()
  framework_types = edge_messages.MlModel.FrameworkValueValuesEnum
  patch_req_type = (
      edge_messages.EdgeProjectsLocationsRegistriesDevicesMlModelsPatchRequest)

  analyze_result = edgeml.EdgeMlClient().Analyze(req.mlModel.modelUri)

  if req.mlModel.framework == framework_types.TFLITE:

    if analyze_result.modelType not in tf_model_types:
      raise exceptions.InvalidArgumentException(
          '--framework', 'tflite provided for non-Tensorflow model.')

    _ProcessTensorflowModel(req.mlModel, args, analyze_result)

    if isinstance(req, patch_req_type):
      # updateMask should have some pre-filled values.
      update_fields = set(req.updateMask.split(','))
      update_fields.update({
          'modelUri', 'acceleratorType', 'inputTensors', 'outputTensors'
      })
      req.updateMask = ','.join(sorted(update_fields))

  # Try to deploy as a scikit-learn model if it's not a TF model.
  elif req.mlModel.framework == framework_types.SCIKIT_LEARN:
    if analyze_result.modelType in tf_model_types:
      raise exceptions.InvalidArgumentException(
          '--framework', 'scikit-learn provided for Tensorflow model.')

  else:
    raise InvalidFrameworkException()  # FRAMEWORK_UNSPECIFIED is not allowed.

  return req


def _ProcessTensorflowModel(model, args, analyze_result):
  """Processes Tensorflow (Lite) model according to analyze result.

  Args:
    model: edge.MlModel message from request
    args: The parsed args namespace from CLI
    analyze_result: edgeml.AnalyzeModelResponse from Analyze method call.

  Raises:
    UncompilableModelException: if given model cannot be optimized for Edge TPU.
  """
  client = edgeml.EdgeMlClient()
  edgeml_messages = edgeml_util.GetMessagesModule()
  edge_messages = edge_util.GetMessagesModule()
  model_types = edgeml_messages.AnalyzeModelResponse.ModelTypeValueValuesEnum
  accelerator_types = edge_messages.MlModel.AcceleratorTypeValueValuesEnum

  model_type = analyze_result.modelType
  model_signature = analyze_result.modelSignature
  edgetpu_compiliability = analyze_result.edgeTpuCompilability

  # Convert method converts TF SavedModel to TF Lite model.
  if model_type == model_types.TENSORFLOW_SAVED_MODEL:
    convert_result, model.modelUri = client.Convert(model.modelUri)
    model_signature = convert_result.modelSignature
    edgetpu_compiliability = convert_result.edgeTpuCompilability
    model_type = model_types.TENSORFLOW_LITE

  if model_type == model_types.TENSORFLOW_LITE:
    # Always use accelerator value from command line, and ignore previous
    # acceleratorType of the model.
    if args.accelerator == 'tpu':
      if edgetpu_compiliability.uncompilableReason:
        raise UncompilableModelException(
            edgetpu_compiliability.uncompilableReason)

      compile_result, model.modelUri = client.Compile(model.modelUri)
      model_signature = compile_result.modelSignature
      model_type = model_types.TENSORFLOW_LITE_EDGE_TPU_OPTIMIZED

  if model_type == model_types.TENSORFLOW_LITE_EDGE_TPU_OPTIMIZED:
    if args.IsSpecified('accelerator') and args.accelerator != 'tpu':
      raise exceptions.InvalidArgumentException(
          '--accelerator',
          'TPU should be provided for Edge TPU optimized model.')
    if not args.IsSpecified('accelerator'):
      log.info('Setting accelerator to TPU for Edge TPU model.')
      model.acceleratorType = accelerator_types.TPU

  _FillModelSignature(model, model_signature)


def _ConvertTensorRef(edgeml_tensor_refs):
  """Converts edgeml.TensorRef[] to edge.TensorInfo[]."""
  edge_messages = edge_util.GetMessagesModule()
  inference_type = edge_messages.TensorInfo.InferenceTypeValueValuesEnum
  edge_tensor_infos = []
  for tensor_ref in edgeml_tensor_refs:
    tensor_info = edge_messages.TensorInfo(
        index=tensor_ref.index,
        dimensions=tensor_ref.tensorInfo.dimensions,
        tensorName=tensor_ref.tensorInfo.tensorName,
        inferenceType=inference_type(tensor_ref.tensorInfo.inferenceType.name))
    edge_tensor_infos.append(tensor_info)
  return edge_tensor_infos


def _FillModelSignature(model, model_signature):
  """Updates ML model's input and output tensors with edgeml.ModelSignature."""
  model.inputTensors = _ConvertTensorRef(model_signature.inputTensors)
  model.outputTensors = _ConvertTensorRef(model_signature.outputTensors)
