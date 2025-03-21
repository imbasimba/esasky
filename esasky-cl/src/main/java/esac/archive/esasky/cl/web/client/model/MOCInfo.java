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

package esac.archive.esasky.cl.web.client.model;

import esac.archive.esasky.cl.web.client.model.entities.MOCEntity;
import esac.archive.esasky.ifcs.model.client.GeneralJavaScriptObject;
import esac.archive.esasky.ifcs.model.descriptor.CommonTapDescriptor;

public class MOCInfo {
    public MOCInfo(CommonTapDescriptor descriptor, MOCEntity entity, int count, GeneralJavaScriptObject pixels) {
        this.descriptor = descriptor;
        this.entity = entity;
        this.count = count;
        this.pixels = pixels;
    }
    public final int count;
    public final GeneralJavaScriptObject pixels;
    public final CommonTapDescriptor descriptor;
    public final MOCEntity entity;
}
