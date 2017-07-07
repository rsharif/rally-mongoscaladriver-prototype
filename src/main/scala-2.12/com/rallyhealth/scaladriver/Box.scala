package com.rallyhealth.scaladriver

import org.joda.time.DateTime

sealed class Box {

  def _id: String = ""

  def length: Int = 1

  def width: Int = 2

  def height: Int = 3

  def manufactureDate: DateTime = DateTime.now()

  def lastShipped: Option[DateTime] = Option.empty
}

case class CorrugatedBox(
  override val _id: String,
  override val length: Int,
  override val width: Int,
  override val height: Int,
  override val manufactureDate: DateTime,
  override val lastShipped: Option[DateTime],
  layers: Int) extends Box

case class RigidBox(
  override val _id: String,
  override val length: Int,
  override val width: Int,
  override val height: Int,
  override val manufactureDate: DateTime,
  override val lastShipped: Option[DateTime],
  numberOfPiece: Int) extends Box

case class FoldingBox(
  override val _id: String,
  override val length: Int,
  override val width: Int,
  override val height: Int,
  override val manufactureDate: DateTime,
  override val lastShipped: Option[DateTime],
  style: String) extends Box

case class BoxOfBoxes(
  override val _id: String,
  override val length: Int,
  override val width: Int,
  override val height: Int,
  override val manufactureDate: DateTime,
  override val lastShipped: Option[DateTime],
  boxes: Seq[Box]) extends Box



