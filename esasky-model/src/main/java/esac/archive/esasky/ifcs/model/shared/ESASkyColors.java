/*
ESASky
Copyright (C) 2025 European Space Agency

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package esac.archive.esasky.ifcs.model.shared;

public class ESASkyColors {
    protected static final Color[] colors = new Color[]{new Color("#E8ECFB", 16), new Color("#D9CCE3", 13.5), new Color("#CAACCB", 13),
				new Color("#BA8DB2", 12), new Color("#AA6F9E", 11), new Color("#994F88", 10), new Color("#882E72", 9),
				new Color("#1965B0", 8), new Color("#437DBF", 7), new Color("#6195CF", 6.8), new Color("#7BAFDE", 6.5),
				new Color("#4EB265", 6.3), new Color("#90C987", 6.2), new Color("#CAE0AB", 6.0), new Color("#F7F056", 5.66),
				new Color("#F7CB45", 5.33), new Color("#F4A736", 5), new Color("#EE8026", 4.5), new Color("#E65518", 4), 
				new Color("#DC050C", 3.0), new Color("#A5170E", 2), new Color("#72190E", 1), new Color("#631006", 0)};
	
	//This will extract the starting colorIndex from the floored -Log10 of the wavelength.
	//Ie UV with log(wl) @ ~- 7 would start at wavelengthIndex[7] 
	// Radar 	0-1
	// submm	1-3
	// IR 		3-6
	// OPtical	6-6.5
	// UV 		6.5-8
	// Soft Xrays 	8-10
	// Hard Xrays 	10-11
	// Gamma	11+
	private static int[] wavelength2ColorIndex;
	
	private static Integer index;
	
	public static String getNext() {
		if(index == null) {
			index = 0;
			return colors[index].color;
		}
		
		index++;
		if(index == colors.length -1 ) {
			index = 0;
		}
		
		return colors[index].color;
	}
	
	public static String getColor(int n){
		return colors[n].color;
	}
	
	public static int maxIndex() {
		return colors.length - 1;
	}
	
	public static String getColorFromWavelength(double wavelength) {
		int index = wavelengthToIndex(wavelength);
		return getColor(index);
	}
	
	private static void initWavelength2ColorIndex() {
		int[] tmp = new int[15];
		int currIndex = 0;
		for(int i = colors.length - 1; i >= 0; i--) {
			if(colors[i].wavelength >= currIndex) {
				tmp[currIndex] = i;
				currIndex++;
			}
		}
		wavelength2ColorIndex = tmp;
	}
	
	public static int wavelengthToIndex(double wavelength) {
		if(wavelength2ColorIndex == null) {
			initWavelength2ColorIndex();
		}
		
		//Should already be in Log10
		if(wavelength < 0) {
			wavelength = - wavelength;
		}
		
		int index = (int) Math.floor(wavelength) + 1;
		if(index >= wavelength2ColorIndex.length - 1) {
			return 0;
		}else if(index < 0) {
			return colors.length - 1;
		}
		
		int startIndex = wavelength2ColorIndex[index];
		int i = startIndex;
		double dist = 2;
		double prevDist = 2;
		while (true) {
			if(i >= colors.length - 1 ) {
				return colors.length - 1;
			}
			dist = colors[i].wavelength - wavelength;
			if(dist < 0) {
				if(Math.abs(dist) < prevDist){
					return i;
				}else {
					return i-1;
				}
			}else {
				prevDist = dist;
				i++;
			}
		}
	}
	
	public static double indexToWaveLength(int index) {
		return colors[index].wavelength;
	}

	public static double valueToWaveLength(double value) {
		int index = (int) Math.floor(value);
		if(index < colors.length - 1) {
			return (value - index) * (colors[index + 1].wavelength - colors[index].wavelength) + colors[index].wavelength;
		}
		return colors[colors.length - 1].wavelength;
	}

	public static String invertColor(String hex) {
		if (!hex.startsWith("#")) {
			return ESASkyColors.getNext();
		}

		hex = hex.replace("#", "");

		// convert 3-digit hex to 6-digits.
		if (hex.length() == 3) {
			hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
		}

		// Invalid color
		if (hex.length() != 6) {
			return hex;
		}

		// invert color components
		String r = Integer.toString (255 - Integer.parseInt(hex.substring(0, 2), 16), 16);
		String g = Integer.toString (255 - Integer.parseInt(hex.substring(2, 4), 16), 16);
		String b = Integer.toString (255 - Integer.parseInt(hex.substring(4, 6), 16), 16);

		// pad each with zeros and return
		return '#' + (r.length() < 2 ? "0" + r : r)
				+ (g.length() < 2 ? "0" + g : g)
				+ (b.length() < 2 ? "0" + b : b);
	}


	private static class Color{
		public final String color;
		public final double wavelength;
		
		public Color(String color, double wavelength) {
			this.color = color;
			this.wavelength = wavelength;
		}
	}
	
}


