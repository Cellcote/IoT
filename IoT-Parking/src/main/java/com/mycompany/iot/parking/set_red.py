from sense_hat import SenseHat

sense = SenseHat()

for x in xrange(0, 7):
    for y in xrange(0, 7):
        sense.set_pixel(x,y, [255,0,0])
