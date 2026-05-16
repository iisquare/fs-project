from typing import Any, Literal, List, Union, Optional

from pydantic import BaseModel


class ClassifyCreateParams(BaseModel):

    model: str

    texts: List[str]

    score: float = 0.65

class CreateClassifyResponse(BaseModel):

    model: str

    predict: List[List[float]]

    classify: List[List[str]]
