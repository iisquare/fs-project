import os
import re
from typing import List

from setuptools import find_packages, setup


def get_version() -> str:
    with open(os.path.join("src", "fs_gpt", "env.py"), encoding="utf-8") as f:
        file_content = f.read()
        pattern = r"{}\W*=\W*\"([^\"]+)\"".format("VERSION")
        (version,) = re.findall(pattern, file_content)
        return version


def get_requires() -> List[str]:
    with open("requirements.txt", encoding="utf-8") as f:
        file_content = f.read()
        lines = [line.strip() for line in file_content.strip().split("\n") if not line.startswith("#")]
        return lines


def get_console_scripts() -> List[str]:
    console_scripts = ["fs-gpt = fs_gpt.cli:main"]
    return console_scripts


extra_require = {
    "torch": ["torch", "torchvision", "torchaudio"],
    "embedding": ["sentence-transformers", "tiktoken"],
    "train": ["deepspeed", "torchinfo", "peft", "trl"],
    "inference": ["vllm", "sgl-kernel", "sglang[all]"],
}
extra_require["all"] = [
    dependency
    for extra_key, dependencies in extra_require.items()
    if extra_key != "all"  # 排除 all 自身，避免循环
    for dependency in dependencies
]
seen = set() # 去重
extra_require["all"] = [dep for dep in extra_require["all"] if not (dep in seen or seen.add(dep))]

def main():
    setup(
        name="fs_gpt",
        version=get_version(),
        author="Ouyang",
        author_email="iisquare AT 163.com",
        description="fs-project python core module",
        long_description=open("README.md", encoding="utf-8").read(),
        long_description_content_type="text/markdown",
        keywords=["fs-project", "fs-gpt"],
        license="Apache 2.0 License",
        url="https://github.com/iisquare/fs-project",
        package_dir={"": "src"},
        packages=find_packages("src"),
        python_requires=">=3.10.0",
        install_requires=get_requires(),
        extras_require=extra_require,
        entry_points={"console_scripts": get_console_scripts()},
        classifiers=[
            "Operating System :: OS Independent",
            "Programming Language :: Python :: 3",
            "Programming Language :: Python :: 3.10",
        ],
    )


if __name__ == "__main__":
    main()
