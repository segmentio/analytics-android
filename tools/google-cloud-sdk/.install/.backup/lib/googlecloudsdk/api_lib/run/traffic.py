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
"""Wrapper fors a Cloud Run TrafficTargets messages."""
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

import collections


class _LatestRevisionKey(object):
  """Key class for the latest revision in TrafficTargets."""

  def __repr__(self):
    return '&&&LATEST&&&'


# Designated key value for latest.
#
# Note this is not a str to avoid conflicts with revision names.
LATEST_REVISION_KEY = _LatestRevisionKey()


def NewTrafficTarget(messages, key, percent):
  if key == LATEST_REVISION_KEY:
    result = messages.TrafficTarget(
        latestRevision=True,
        percent=percent)
  else:
    result = messages.TrafficTarget(
        revisionName=key,
        percent=percent)
  return result


def GetKey(target):
  """Returns the key for a TrafficTarget.

  Args:
    target: TrafficTarget, the TrafficTarget to check

  Returns:
    LATEST_REVISION_KEY if target is for the latest revison or
    target.revisionName if not.
  """
  return LATEST_REVISION_KEY if target.latestRevision else target.revisionName


def SortKeyFeyFromKey(key):
  """Sorted key function  to order TrafficTarget keys.

  TrafficTargets keys are one of:
  o revisionName
  o LATEST_REVISION_KEY

  Note LATEST_REVISION_KEY is not a str so its ordering with respect
  to revisionName keys is hard to predict.

  Args:
    key: Key for a TrafficTargets dictionary.

  Returns:
    A value that sorts by revisionName with LATEST_REVISION_KEY
    last.
  """
  if key == LATEST_REVISION_KEY:
    result = (2, key)
  else:
    result = (1, key)
  return result


def SortKeyFeyFromTarget(target):
  """Sorted key function to order TrafficTarget objects by key.

  Args:
    target: A TrafficTarget.

  Returns:
    A value that sorts by revisionName with LATEST_REVISION_KEY
    last.
  """
  key = GetKey(target)
  return SortKeyFeyFromKey(key)


def NewRoundingCorrectionPrecedence(key_and_percent):
  """Returns object that sorts in the order we correct traffic rounding errors.

  The caller specifies explicit traffic percentages for some revisions and
  this module scales traffic for remaining revisions that are already
  serving traffic up or down to assure that 100% of traffic is assigned.
  This scaling can result in non integrer percentages that Cloud Run
  does not supprt. We correct by:
    - Trimming the decimal part of float_percent, int(float_percent)
    - Adding an extra 1 percent traffic to enough revisions that have
      had their traffic reduced to get us to 100%

  The returned value sorts in the order we correct revisions:
    1) Revisions with a bigger loss due are corrected before revisions with
       a smaller loss. Since 0 <= loss < 1 we sort by the value:  1 - loss.
    2) In the case of ties revisions with less traffic are corrected before
       revisions with more traffic.
    3) In case of a tie revisions with a smaller key are corrected before
       revisions with a larger key.

  Args:
    key_and_percent: tuple with (key, float_percent)

  Returns:
    An value that sorts with respect to values returned for
    other revisions in the order we correct for rounding
    errors.
  """
  key, float_percent = key_and_percent
  return [
      1 - (float_percent - int(float_percent)),
      float_percent,
      SortKeyFeyFromKey(key)]


class TrafficTargets(collections.MutableMapping):
  """Wraps a repeated TrafficTarget message and provides dict-like access.

  The dictionary key is one of
     LATEST_REVISION_KEY for the latest revision
     TrafficTarget.revisionName for TrafficTargets with a revision name.

  """

  def __init__(
      self, messages_module, to_wrap):
    """Constructor.

    Args:
      messages_module: The message module that defines TrafficTarget.
      to_wrap: The traffic targets to wrap.
    """
    self._messages = messages_module
    self._m = to_wrap
    self._traffic_target_cls = self._messages.TrafficTarget

  def __getitem__(self, key):
    """Implements evaluation of `self[key]`."""
    for target in self._m:
      if key == GetKey(target):
        return target
    raise KeyError(key)

  def __setitem__(self, key, new_target):
    """Implements evaluation of `self[key] = target`."""
    for index, target in enumerate(self._m):
      if key == GetKey(target):
        self._m[index] = new_target
        break
    else:
      self._m.append(new_target)

  def __delitem__(self, key):
    """Implements evaluation of `del self[key]`."""
    index_to_delete = 0
    for index, target in enumerate(self._m):
      if key == GetKey(target):
        index_to_delete = index
        break
    else:
      raise KeyError(key)

    del self._m[index_to_delete]

  def __contains__(self, key):
    """Implements evaluation of `item in self`."""
    for target in self._m:
      if key == GetKey(target):
        return True
    return False

  def __len__(self):
    """Implements evaluation of `len(self)`."""
    return len(self._m)

  def __iter__(self):
    """Returns a generator yielding the env var keys."""
    for target in self._m:
      yield GetKey(target)

  def MakeSerializable(self):
    return self._m

  def __repr__(self):
    content = ', '.join('{}: {}'.format(k, v) for k, v in self.items())
    return '[%s]' % content

  def _ValidateCurrentTraffic(self):
    percent = 0
    for target in self._m:
      percent += target.percent

    if percent != 100:
      raise ValueError(
          'Current traffic allocation of %s is not 100 percent' % percent)

    for target in self._m:
      if target.percent < 0:
        raise ValueError(
            'Current traffic for target %s is negative (%s)' % (
                GetKey(target), target.percent))

  def _GetUnassignedTargets(self, new_percentages):
    """Get TrafficTargets with traffic not in new_percentages."""
    result = {}
    for target in self._m:
      key = GetKey(target)
      if target.percent and key not in new_percentages:
        result[key] = target
    return result

  def _IsChangedPercentages(self, new_percentages):
    """Returns True iff new_percentages changes current traffic."""
    old_percentages = {GetKey(target): target.percent for target in self._m}
    for key in new_percentages:
      if (key not in old_percentages or
          new_percentages[key] != old_percentages[key]):
        return True
    return False

  def _ValidateNewPercentages(self, new_percentages, unspecified_targets):
    """Validate the new traffic percentages the user specified."""
    if not self._IsChangedPercentages(new_percentages):
      raise ValueError('No traffic changes specified.')

    specified_percent = sum(new_percentages.values())
    if specified_percent > 100:
      raise ValueError('Over 100% of traffic is specified.')

    for key in new_percentages:
      if new_percentages[key] < 0 or new_percentages[key] > 100:
        raise ValueError(
            'New traffic for target %s is %s, not between 0 and 100' % (
                key, new_percentages[key]))

    if not unspecified_targets and specified_percent < 100:
      raise ValueError(
          'Every target with traffic is updated but 100% of '
          'traffic has not been specified.')

  def _GetPercentUnspecifiedTraffic(self, new_percentages):
    """Returns percentage of traffic not explicitly specified by caller."""
    specified_percent = sum(new_percentages.values())
    return 100 - specified_percent

  def _IntPercentages(self, float_percentages):
    rounded_percentages = {
        k: int(float_percentages[k]) for k in float_percentages}
    loss = int(round(sum(float_percentages.values()))) - sum(
        rounded_percentages.values())
    correction_precedence = sorted(
        float_percentages.items(),
        key=NewRoundingCorrectionPrecedence)
    for key, _ in correction_precedence[:loss]:
      rounded_percentages[key] += 1
    return rounded_percentages

  def _GetAssignedPercentages(self, new_percentages, unassigned_targets):
    percent_to_assign = self._GetPercentUnspecifiedTraffic(new_percentages)
    if percent_to_assign == 0:
      return {}
    percent_to_assign_from = sum(
        target.percent for target in unassigned_targets.values())
    #
    # We assign traffic to unassigned targests (were seving and
    # have not explicit new percentage assignent). The assignment
    # is proportional to the original traffic for the each target.
    #
    # percent_to_assign
    #    == percent_to_assign_from * (
    #          percent_to_assign)/percent_to_assign_from)
    #    == sum(unassigned_targets[k].percent) * (
    #          percent_to_assign)/percent_to_assign_from)
    #    == sum(unassigned_targets[k].percent] *
    #          percent_to_assign)/percent_to_assign_from)
    assigned_percentages = {}
    for k in unassigned_targets:
      assigned_percentages[k] = unassigned_targets[k].percent * float(
          percent_to_assign)/percent_to_assign_from
    return assigned_percentages

  def UpdateTraffic(self, new_percentages, new_latest_percentage):
    """Update traffic assignments.

    The updated traffic assignments will include assignments explicitly
    specified by the caller. If the caller does not assign 100% of
    traffic explicitly this function will scale traffic for targets
    the user does not specify up or down based on the provided
    assignments as needed.

    The update removes targets with 0% traffic unless:
     o The user explicitly specifies under 100% of total traffic
     o The user does not explicitly specify 0% traffic for the target.

    Args:
      new_percentages: Dict[str, int], Map from revision to percent
        traffric for the revision.
      new_latest_percentage: int, Percent traffic to assign to the
        latest revision or None to not explicitly specify.
    Raises:
      ValueError: if the specified traffic settings are not valid.
    """
    self._ValidateCurrentTraffic()
    original_targets = {GetKey(target): target for target in self._m}
    updated_percentages = new_percentages.copy()
    if new_latest_percentage is not None:
      updated_percentages[LATEST_REVISION_KEY] = new_latest_percentage
    unassigned_targets = self._GetUnassignedTargets(updated_percentages)
    self._ValidateNewPercentages(updated_percentages, unassigned_targets)
    updated_percentages.update(
        self._GetAssignedPercentages(updated_percentages, unassigned_targets))
    int_percentages = self._IntPercentages(updated_percentages)
    new_targets = []
    for key in int_percentages:
      if (key in new_percentages and new_percentages[key] == 0) or (
          key == LATEST_REVISION_KEY and new_latest_percentage == 0):
        continue
      elif key in original_targets:
        # Preserve state of retained targets.
        target = original_targets[key]
        target.percent = int_percentages[key]
      else:
        target = NewTrafficTarget(self._messages, key, int_percentages[key])
      new_targets.append(target)
    new_targets = sorted(new_targets, key=SortKeyFeyFromTarget)
    del self._m[:]
    self._m.extend(new_targets)
