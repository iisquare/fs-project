from abc import abstractmethod
from typing import Dict


class Actuator:
    def __init__(self, args: Dict) -> None:
        self.args = args

    @abstractmethod
    def train(self):
        pass

    @abstractmethod
    def predict(self):
        pass

def main(args: Dict):
    match args.get("actuator"):
        case "classify":
            from fs_gpt.bert.classify import ClassifyActuator
            actuator = ClassifyActuator(args)
        case _:
            raise Exception(f"Unknown actuator: {args.get('actuator')}")
    match args.get("stage"):
        case "train":
            actuator.train()
        case "predict":
            actuator.predict()
        case _:
            raise Exception(f"Unknown stage: {args.get('stage')}")
