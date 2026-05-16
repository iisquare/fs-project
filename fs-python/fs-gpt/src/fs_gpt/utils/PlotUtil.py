import json
import math
import os
from typing import Any, Dict, List

from transformers.trainer import TRAINER_STATE_NAME

from fs_gpt.utils.PackageUtil import PackageUtil

if PackageUtil.available("matplotlib"):
    import matplotlib.figure
    import matplotlib.pyplot as plt


class PlotUtil:
    @staticmethod
    def smooth(scalars: List[float]) -> List[float]:
        r"""
        EMA implementation according to TensorBoard.
        """
        if len(scalars) == 0:
            return []

        last = scalars[0]
        smoothed = []
        weight = 1.8 * (1 / (1 + math.exp(-0.05 * len(scalars))) - 0.5)  # a sigmoid function
        for next_val in scalars:
            smoothed_val = last * weight + (1 - weight) * next_val
            smoothed.append(smoothed_val)
            last = smoothed_val
        return smoothed

    @staticmethod
    def gen_loss_plot(trainer_log: List[Dict[str, Any]]) -> "matplotlib.figure.Figure":
        r"""
        Plots loss curves in LlamaBoard.
        """
        plt.close("all")
        plt.switch_backend("agg")
        fig = plt.figure()
        ax = fig.add_subplot(111)
        steps, losses = [], []
        for log in trainer_log:
            if log.get("loss", None):
                steps.append(log["current_steps"])
                losses.append(log["loss"])

        ax.plot(steps, losses, color="#1f77b4", alpha=0.4, label="original")
        ax.plot(steps, PlotUtil.smooth(losses), color="#1f77b4", label="smoothed")
        ax.legend()
        ax.set_xlabel("step")
        ax.set_ylabel("loss")
        return fig

    @staticmethod
    def plot_loss(save_dictionary: str, keys: List[str] = ["loss"]) -> None:
        r"""
        Plots loss curves and saves the image.
        """
        plt.switch_backend("agg")
        with open(os.path.join(save_dictionary, TRAINER_STATE_NAME), encoding="utf-8") as f:
            data = json.load(f)

        for key in keys:
            steps, metrics = [], []
            for i in range(len(data["log_history"])):
                if key in data["log_history"][i]:
                    steps.append(data["log_history"][i]["step"])
                    metrics.append(data["log_history"][i][key])

            plt.figure()
            plt.plot(steps, metrics, color="#1f77b4", alpha=0.4, label="original")
            plt.plot(steps, PlotUtil.smooth(metrics), color="#1f77b4", label="smoothed")
            plt.title(f"training {key} of {save_dictionary}")
            plt.xlabel("step")
            plt.ylabel(key)
            plt.legend()
            figure_path = os.path.join(save_dictionary, "training_{}.png".format(key.replace("/", "_")))
            plt.savefig(figure_path, format="png", dpi=100)
            print("Figure saved at:", figure_path)
