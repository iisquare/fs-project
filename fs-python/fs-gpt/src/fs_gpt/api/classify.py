from typing import TYPE_CHECKING, Dict

from fastapi import APIRouter, Depends, status

from fs_gpt.bert.classify import ClassifyActuator
from fs_gpt.protocol.classify import ClassifyCreateParams, CreateClassifyResponse

if TYPE_CHECKING:
    from fs_gpt.api.server import Server


def create_router(app: "Server", router: APIRouter, args: Dict) -> APIRouter:
    kwargs = {
        "model_name_or_path": args.get("classify_name_or_path"),
        "classify_labels": args.get("classify_labels"),
    }
    if "classify_device" in args:
        kwargs["device"] = args.get("classify_device")
    actuator = ClassifyActuator(kwargs)
    actuator.load_checkpoint(kwargs.get("model_name_or_path")).eval()

    @router.post(
        "/classify",
        dependencies=[Depends(app.check_api_key)],
        status_code=status.HTTP_200_OK,
    )
    async def create_classify(request: ClassifyCreateParams):
        predict, classify = actuator.classify(request.texts, request.score)
        return CreateClassifyResponse(
            model=request.model,
            predict=predict,
            classify=classify,
        )
    return router
