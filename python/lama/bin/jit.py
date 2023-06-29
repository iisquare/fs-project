#!/usr/bin/env python3

# Example command:
# python bin/jit.py model.path=$(pwd)/LaMa_models/lama-places/lama-fourier

import os

os.environ['OMP_NUM_THREADS'] = '1'
os.environ['OPENBLAS_NUM_THREADS'] = '1'
os.environ['MKL_NUM_THREADS'] = '1'
os.environ['VECLIB_MAXIMUM_THREADS'] = '1'
os.environ['NUMEXPR_NUM_THREADS'] = '1'

import hydra
import torch
import yaml
from omegaconf import OmegaConf

from saicinpainting.training.trainers import load_checkpoint

out_path = os.path.join(os.path.abspath(os.getcwd()), 'LaMa_models/lama.pt')

@hydra.main(config_path='../configs/prediction', config_name='default.yaml')
def main(predict_config: OmegaConf):

    train_config_path = os.path.join(predict_config.model.path, 'config.yaml')
    with open(train_config_path, 'r') as f:
        train_config = OmegaConf.create(yaml.safe_load(f))

    train_config.training_model.predict_only = True
    train_config.visualizer.kind = 'noop'

    checkpoint_path = os.path.join(predict_config.model.path,
                                   'models',
                                   predict_config.model.checkpoint)
    model = load_checkpoint(train_config, checkpoint_path, strict=False, map_location='cpu')
    model.freeze()

    inputs = {'image': torch.ones(1, 3, 600, 800), 'mask': torch.ones(1, 1, 600, 800)}
    model = torch.jit.trace(model, inputs, strict=False)
    torch.jit.save(model, out_path)
    print('save model to %s' % out_path)


if __name__ == '__main__':
    main()
