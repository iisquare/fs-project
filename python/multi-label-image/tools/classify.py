import os

dataset_dir = r'C:\Users\Ouyang\Desktop\dataset\classify'

if __name__ == '__main__':

    for dir_name in os.listdir(dataset_dir):
        dir_path = os.path.join(dataset_dir, dir_name)
        if not os.path.isdir(dir_path):
            continue
        for file_name in os.listdir(dir_path):
            file_path = os.path.join(dir_path, file_name)
            if not os.path.isfile(file_path):
                continue
            if not file_name.endswith('.jpg'):
                continue
            print("{}/{}".format(dir_name, file_name))
