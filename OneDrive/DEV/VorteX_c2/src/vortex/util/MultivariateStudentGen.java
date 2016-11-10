/*
 * Copyright (C) 2015 Nikolay
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package vortex.util;

import umontreal.iro.lecuyer.randvar.ChiSquareGen;
import umontreal.iro.lecuyer.randvar.NormalACRGen;
import umontreal.iro.lecuyer.randvar.StudentNoncentralGen;
import umontreal.iro.lecuyer.randvarmulti.RandomMultivariateGen;
import umontreal.iro.lecuyer.rng.GenF2w32;
import umontreal.iro.lecuyer.rng.LFSR113;
import umontreal.iro.lecuyer.rng.MRG31k3p;
import umontreal.iro.lecuyer.rng.MRG32k3a;

/**
 *
 * @author Nikolay
 */
public class MultivariateStudentGen extends RandomMultivariateGen{
    double [] mode;
    double [] sigma;
    int deg_fr;
    
    StudentNoncentralGen gen;
    
    public MultivariateStudentGen(double[] mu, double[] sigma, int deg_freedom){
        this.mode = mu;
        this.sigma = sigma;
        assert(mode.length==sigma.length);
        deg_fr = deg_freedom;
        gen = new StudentNoncentralGen(new NormalACRGen(new MRG31k3p()), new ChiSquareGen(new MRG31k3p(), deg_fr));
    }
    
    @Override
    public void nextPoint(double[] p) {
        assert(p.length==mode.length);
        for (int i = 0; i < p.length; i++) {
            p[i] = (gen.nextDouble()*sigma[i])+mode[i];
            
        }
    }
    
    
}
