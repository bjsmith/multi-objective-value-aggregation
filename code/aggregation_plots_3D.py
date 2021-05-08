import matplotlib
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.cm import ScalarMappable

def ELA(X):
    return -np.exp(-X) + 1

def LELA(X):
    return -np.exp(-X) + X + 1

def SFLLA(X):
    return np.where(X <= 0, -np.exp(-X) + 1, np.log(X + 1))

def SFMLA(X):
    return np.where(X <= 0, 2*X, X)

def ROLAND_UTIL(X, mu):
    return X - (X - mu)**2

def ROLAND_SAFETY(X):
    return X**2

#agg_names = ["MIN", "ELA", "LELA", "SFLLA", "SFMLA"]
agg_names = ["ROLAND1_3D", "ROLAND2_3D"]
#agg_functions = [ELA, LELA, SFLLA, SFMLA]
agg_functions = []
up_range = np.linspace(-3, 3, 100)
ua_range = np.linspace(-3, 3, 100)

X, Y = np.meshgrid(up_range, ua_range)
Zmin = np.minimum(X,Y)
mu = 0.5*(X + Y)
Zroland1 = ROLAND_UTIL(X, mu) + ROLAND_UTIL(Y, mu)
Zroland2 = ROLAND_UTIL(X, X) + ROLAND_SAFETY(Y)

Zs = [Zroland1, Zroland2]

for ai, af in enumerate(agg_functions):
    print(ai)
    Z = 0.5*(af(X) + af(Y))
    Zs.append(Z)

for zi, Z in enumerate(Zs):
    levels = 20
    Zmaxabs = np.abs(min(Z.min(), Z.max()))
    #level_boundaries = np.linspace(vmin, vmax, levels + 1)
    fig = plt.figure()
    """
    lines = plt.contourf(X, Y, Z, levels, cmap="RdGy", vmin=-Zmaxabs, vmax=Zmaxabs)#, norm=matplotlib.colors.LogNorm())
    cbar = plt.colorbar()
    cbar.ax.get_yaxis().labelpad = 15
    cbar.ax.set_ylabel(r"$f(\vec{U})$", rotation=270)

    lines = plt.contour(X, Y, Z, levels, colors="black", vmin=-Zmaxabs, vmax=Zmaxabs)#, norm=matplotlib.colors.LogNorm())
    plt.clabel(lines, inline=True, fmt='%2.1f', colors='black', fontsize=8)
    #plt.plot(X, Y, marker="o", color="black", linestyle="")
    #plt.clabel(contours, inline=True, fontsize=8,fmt = ticker.LogFormatterMathtext())
    #plt.imshow(Z, extent=[nhid_list[0], nhid_list[-1], n_list[0], n_list[-1]], alpha=0.5)
    """
    ax = plt.axes(projection='3d')
    ax.plot_surface(X, Y, Z, rstride=1, cstride=1, cmap='RdGy', vmin=-Zmaxabs, vmax=Zmaxabs, edgecolor="none")
    #ax.contour3D(X, Y, Z, levels, cmap='RdGy', vmin=-Zmaxabs, vmax=Zmaxabs, edgecolor="none")
    #ax.plot_wireframe(X, Y, Z, cmap='RdGy')
    ax.set_xlabel('$U_1$')
    ax.set_ylabel('$U_2$')
    ax.set_zlabel(r'$f(\vec{U})$');

    plt.ylabel(r"$U_1$")
    plt.xlabel(r"$U_2$")


    plt.savefig("aggplots/{}.pdf".format(agg_names[zi]), bbox_inches='tight')
plt.show()
