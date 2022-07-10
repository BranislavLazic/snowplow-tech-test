package com.snowplow.model

case class ServiceResponse(action: String, id: String, status: String, message: Option[String] = None)
