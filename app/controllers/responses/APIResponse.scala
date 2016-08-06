package controllers.responses
import play.api.libs.json.{Json, Format, JsValue, JsNull, Writes}

case class ErrorResult (
	status: Int,
	message: String
)

object ErrorResult {
	implicit val format: Format[ErrorResult] = Json.format[ErrorResult]
}

case class APIResponse (
	result: String,
	response: JsValue,
	error: Option[ErrorResult]
)

object APIResponse {
	implicit val format: Format[APIResponse] = Json.format[APIResponse]
}

object ErrorResponse {
	val INVALID_JSON = 1000
	val NOT_ENOUGH_TICKETS = 1001

	def apply(status: Int, message: String) = {
		APIResponse("ko", JsNull, Option(ErrorResult(status, message)))
	}
}

object SuccessResponse {
	def apply[A](successResponse: A)(implicit w: Writes[A]) = {
		APIResponse("ok", Json.toJson(successResponse), None)
	}
}