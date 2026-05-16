import importlib.metadata
import importlib.util

from packaging import version


class PackageUtil:
    @staticmethod
    def available(name: str) -> bool:
        return importlib.util.find_spec(name) is not None

    @staticmethod
    def version(name: str) -> version.Version:
        return version.parse(importlib.metadata.version(name))
