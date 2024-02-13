package com.batodev.jigsawpuzzle.cut;

public class PuzzleCurvesGenerator {
    public String generateSvg() {
        var data = "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.0\" ";
        data += "width=\"" + width + "\" height=\"" + height + "\" viewBox=\"0 0 " + width + " " + height + "\">\n";
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\"";
        data += gen_dh();
        data += "\" />\n";
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\"";
        data += gen_dv();
        data += "\" />\n";
        data += "<path fill=\"none\" stroke=\"Black\" stroke-width=\"1\" d=\"";
        data += gen_db();
        data += "\" />\n";
        data += "</svg>";
        return data;
    }

    public double seed = 2;
    public double a, b, c, d, e, t = 0.1, j = 0.05, xi, yi, xn, yn, offset, width, height, radius;
    public boolean flip, vertical;

    public double random() {
        var x = Math.sin(seed) * 10000;
        seed += 1;
        return x - Math.floor(x);
    }

    public double uniform(double min, double max) {
        var r = random();
        return min + r * (max - min);
    }

    public boolean rBool() {
        return random() > 0.5;
    }

    public void first() {
        e = uniform(-j, j);
        next();
    }

    public void next() {
        var flipold = flip;
        flip = rBool();
        a = (flip == flipold ? -e : e);
        b = uniform(-j, j);
        c = uniform(-j, j);
        d = uniform(-j, j);
        e = uniform(-j, j);
    }

    public double sl() {
        return vertical ? height / yn : width / xn;
    }

    public double sw() {
        return vertical ? width / xn : height / yn;
    }

    public double ol() {
        return offset + sl() * (vertical ? yi : xi);
    }

    public double ow() {
        return offset + sw() * (vertical ? xi : yi);
    }

    public double l(double v) {
        var ret = ol() + sl() * v;
        return (double) Math.round(ret * 100) / 100;
    }

    public double w(double v) {
        var ret = ow() + sw() * v * (flip ? -1.0 : 1.0);
        return (double) Math.round(ret * 100) / 100;
    }

    public double p0l() {
        return l(0.0);
    }

    public double p0w() {
        return w(0.0);
    }

    public double p1l() {
        return l(0.2);
    }

    public double p1w() {
        return w(a);
    }

    public double p2l() {
        return l(0.5 + b + d);
    }

    public double p2w() {
        return w(-t + c);
    }

    public double p3l() {
        return l(0.5 - t + b);
    }

    public double p3w() {
        return w(t + c);
    }

    public double p4l() {
        return l(0.5 - 2.0 * t + b - d);
    }

    public double p4w() {
        return w(3.0 * t + c);
    }

    public double p5l() {
        return l(0.5 + 2.0 * t + b - d);
    }

    public double p5w() {
        return w(3.0 * t + c);
    }

    public double p6l() {
        return l(0.5 + t + b);
    }

    public double p6w() {
        return w(t + c);
    }

    public double p7l() {
        return l(0.5 + b + d);
    }

    public double p7w() {
        return w(-t + c);
    }

    public double p8l() {
        return l(0.8);
    }

    public double p8w() {
        return w(e);
    }

    public double p9l() {
        return l(1.0);
    }

    public double p9w() {
        return w(0.0);
    }

    public String gen_dh() {
        StringBuilder str = new StringBuilder();
        vertical = false;

        for (yi = 1; yi < yn; ++yi) {
            xi = 0;
            first();
            str.append("M ").append(p0l()).append(",").append(p0w()).append(" ");
            for (; xi < xn; ++xi) {
                str.append("C ").append(p1l()).append(" ").append(p1w()).append(" ").append(p2l()).append(" ").append(p2w()).append(" ").append(p3l()).append(" ").append(p3w()).append(" ");
                str.append("C ").append(p4l()).append(" ").append(p4w()).append(" ").append(p5l()).append(" ").append(p5w()).append(" ").append(p6l()).append(" ").append(p6w()).append(" ");
                str.append("C ").append(p7l()).append(" ").append(p7w()).append(" ").append(p8l()).append(" ").append(p8w()).append(" ").append(p9l()).append(" ").append(p9w()).append(" ");
                next();
            }
        }
        return str.toString();
    }

    public String gen_dv() {
        StringBuilder str = new StringBuilder();
        vertical = true;

        for (xi = 1; xi < xn; ++xi) {
            yi = 0;
            first();
            str.append("M ").append(p0w()).append(",").append(p0l()).append(" ");
            for (; yi < yn; ++yi) {
                str.append("C ").append(p1w()).append(" ").append(p1l()).append(" ").append(p2w()).append(" ").append(p2l()).append(" ").append(p3w()).append(" ").append(p3l()).append(" ");
                str.append("C ").append(p4w()).append(" ").append(p4l()).append(" ").append(p5w()).append(" ").append(p5l()).append(" ").append(p6w()).append(" ").append(p6l()).append(" ");
                str.append("C ").append(p7w()).append(" ").append(p7l()).append(" ").append(p8w()).append(" ").append(p8l()).append(" ").append(p9w()).append(" ").append(p9l()).append(" ");
                next();
            }
        }
        return str.toString();
    }

    public String gen_db() {
        var str = "";

        str += "M " + (offset + radius) + " " + (offset) + " ";
        str += "L " + (offset + width - radius) + " " + (offset) + " ";
        str += "A " + (radius) + " " + (radius) + " 0 0 1 " + (offset + width) + " " + (offset + radius) + " ";
        str += "L " + (offset + width) + " " + (offset + height - radius) + " ";
        str += "A " + (radius) + " " + (radius) + " 0 0 1 " + (offset + width - radius) + " " + (offset + height) + " ";
        str += "L " + (offset + radius) + " " + (offset + height) + " ";
        str += "A " + (radius) + " " + (radius) + " 0 0 1 " + (offset) + " " + (offset + height - radius) + " ";
        str += "L " + (offset) + " " + (offset + radius) + " ";
        str += "A " + (radius) + " " + (radius) + " 0 0 1 " + (offset + radius) + " " + (offset) + " ";
        return str;
    }
}
