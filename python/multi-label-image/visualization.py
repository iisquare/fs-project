from keras.optimizers import SGD
from quiver_engine import server  # https://github.com/keplr-io/quiver
import config
from models import net

model = net.dense(config.IMAGE_WIDTH, config.IMAGE_HEIGHT, len(config.CLASS_NAMES))

# let's train the model using SGD + momentum (how original).
sgd = SGD(lr=0.001, decay=1e-6, momentum=0.9, nesterov=False)

model.compile(loss='binary_crossentropy', optimizer=sgd, metrics=['accuracy'])
model.load_weights("weights.hdf5")

server.launch(model, input_folder='./', temp_folder='./filters')
