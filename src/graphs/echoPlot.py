"""
A python program to make a graph called R1. Considering our measurements as Round Trip Time (RTT)
values, Smooth Round Trip Time (SRTT), Round Trip Time Variance (σ) and Retransmission Timeout (RTO) 
are computed and plotted.
"""

import matplotlib.pyplot as plt
import numpy as np

rtt = np.loadtxt('echoResultsDelayed.txt')
a = 0.875
b = 0.75
c = 4

srtt = [None] * len(rtt)
srtt[0] = (1-a) * rtt[0]
for i in range(1, len(rtt)):
    srtt[i] = a * srtt[i-1] + (1-a) * rtt[i]

sigma = [None] * len(rtt)
sigma[0] = (1-b) * abs(srtt[i]-rtt[i])
for i in range(1, len(rtt)):
    sigma[i] = b*sigma[i-1] + (1-b) * abs(srtt[i]-rtt[i])

rto = [None] * len(rtt)
for i in range(len(rtt)):
    rto[i] = srtt[i] + c * sigma[i]

plt.plot(rtt, label="RTT")
plt.plot(srtt, label="SRTT")
plt.plot(sigma, label="σ")
plt.plot(rto, label="RTO")
plt.legend()
plt.savefig('R1.png', dpi=1000)
plt.show()

