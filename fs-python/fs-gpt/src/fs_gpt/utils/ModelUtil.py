from typing import List

from transformers import PreTrainedModel


class ModelUtil:
    @staticmethod
    def find_all_linear_modules(model: PreTrainedModel, freeze_vision_tower: bool) -> List[str]:
        r"""
        Finds all available modules to apply lora or galore.
        """
        model_type = getattr(model.config, "model_type", None)
        forbidden_modules = {"lm_head"}
        if model_type == "chatglm":
            forbidden_modules.add("output_layer")
        elif model_type == "internlm2":
            forbidden_modules.add("output")
        elif model_type in ["llava", "llava_next", "llava_next_video", "mllama", "paligemma", "video_llava"]:
            forbidden_modules.add("multi_modal_projector")
        elif model_type == "qwen2_vl":
            forbidden_modules.add("merger")

        if freeze_vision_tower:
            if model_type == "mllama":
                forbidden_modules.add("vision_model")
            elif model_type == "qwen2_vl":
                forbidden_modules.add("visual")
            else:
                forbidden_modules.add("vision_tower")

        module_names = set()
        for name, module in model.named_modules():
            if any(forbidden_module in name for forbidden_module in forbidden_modules):
                continue

            if "Linear" in module.__class__.__name__ and "Embedding" not in module.__class__.__name__:
                module_names.add(name.split(".")[-1])
        return list(module_names)
