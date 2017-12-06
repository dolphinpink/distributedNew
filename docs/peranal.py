import numpy as np 
import pylab as pl 
x = [10, 12, 50, 150, 500, 1000, 1800]
y = [50, 60, 75, 100, 200, 1000, 2000]


pl.title('Performance Analysis of Distributed Reservation System')
pl.xlabel('Average Response Time (ms)')
pl.ylabel('Load in Number of Clients (txn/s)')

pl.xlim(0, 2000)
pl.ylim(0, 2000)

pl.plot(x, y, 'g--')
pl.show()