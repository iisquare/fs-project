package com.iisquare.fs.web.xlab.service;

import org.opencv.core.Mat;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class Transform360Service {

    public void help() {
        System.out.println("The six cubemap images are in Space Nerds in Space numbering format:");
        System.out.println("         +------+");
        System.out.println("         |  4   |");
        System.out.println("         |      |");
        System.out.println("  +------+------+------+------+");
        System.out.println("  |  0   |  1   |  2   |  3   |");
        System.out.println("  |      |      |      |      |");
        System.out.println("  +------+------+------+------+");
        System.out.println("         |  5   |");
        System.out.println("         |      |");
        System.out.println("         +------+");
        System.out.println("No:left front right back top bottom");
    }

    public Mat cubic(Map<String, Mat> map) {
        Mat result = null;
        for (Map.Entry<String, Mat> entry : map.entrySet()) {
            Mat item = entry.getValue();
            if(null == result) {
                result = new Mat(item.rows() * 3, item.cols() * 4, item.type());
            }
            switch (entry.getKey()) {
                case "left":
                    item.copyTo(result.submat(item.rows(), item.rows() * 2, 0, item.cols()));
                    break;
                case "front":
                    item.copyTo(result.submat(item.rows(), item.rows() * 2, item.cols(), item.cols() * 2));
                    break;
                case "right":
                    item.copyTo(result.submat(item.rows(), item.rows() * 2, item.cols() * 2, item.cols() * 3));
                    break;
                case "back":
                    item.copyTo(result.submat(item.rows(), item.rows() * 2, item.cols() * 3, item.cols() * 4));
                    break;
                case "top":
                    item.copyTo(result.submat(0, item.rows(), item.cols(), item.cols() * 2));
                    break;
                case "bottom":
                    item.copyTo(result.submat(item.rows() * 2, item.rows() * 3, item.cols(), item.cols() * 2));
                    break;
                default:
                    continue;
            }
        }
        return result;
    }

    public double[] angular_position(double[] texcoord) {
        double u = texcoord[0], v = texcoord[1];
        // theta: u: 0..1 -> -pi..pi
        double theta = Math.PI*2.0*(u-0.5);
        // phi: v: 0..1 - > -pi/2..pi/2
        double phi = Math.PI*(v-0.5);
        return new double[]{theta, phi};
    }

    public double[] point_on_sphere(double theta, double phi) {
        double r = Math.cos(phi);
        return new double[]{r*Math.cos(theta), r*Math.sin(theta), Math.sin(phi)};
    }

    public double[] get_pixel_from_uv(double u, double v, Mat mat) {
        int x = (int) (mat.cols() * u), y = (int) (mat.rows() * v);
        x = Math.min(x, mat.cols() - 1);
        y = Math.min(y, mat.rows() - 1);
        return mat.get(y, x);
    }

    public double[] pixel_value(double[] angle, Map<String, Mat> map) {
        double theta = angle[0], phi = angle[1];
        double[] sphere_pnt = point_on_sphere(theta, phi);
        double x = sphere_pnt[0], y = sphere_pnt[1], z = sphere_pnt[2];
        double eps = 1e-6;
        if (Math.abs(x)>eps) {
            if(x > 0) {
                double t = 0.5/x;
                double u = 0.5+t*y;
                double v = 0.5+t*z;
                if(u>=0.0 && u<=1.0 && v>=0.0 && v<=1.0) {
                    return get_pixel_from_uv(u, v, map.get("front"));
                }
            } else if (x<0) {
                double t = 0.5 / -x;
                double u = 0.5 + t * -y;
                double v = 0.5 + t * z;
                if (u >= 0.0 && u <= 1.0 && v >= 0.0 && v <= 1.0) {
                    return get_pixel_from_uv(u, v, map.get("back"));
                }
            }
        }
        if (Math.abs(y)>eps) {
            if (y > 0) {
                double t = 0.5 / y;
                double u = 0.5 + t * -x;
                double v = 0.5 + t * z;
                if (u >= 0.0 && u <= 1.0 && v >= 0.0 && v <= 1.0) {
                    return get_pixel_from_uv(u, v, map.get("right"));
                }
            } else if (y < 0) {
                double t = 0.5 / -y;
                double u = 0.5 + t * x;
                double v = 0.5 + t * z;
                if (u >= 0.0 && u <= 1.0 && v >= 0.0 && v <= 1.0) {
                    return get_pixel_from_uv(u, v, map.get("left"));
                }
            }
        }
        if (Math.abs(z)>eps) {
            if (z > 0) {
                double t = 0.5/z;
                double u = 0.5+t*y;
                double v = 0.5+t*-x;
                if (u >= 0.0 && u <= 1.0 && v >= 0.0 && v <= 1.0) {
                    return get_pixel_from_uv(u, v, map.get("bottom"));
                }
            } else if (z < 0) {
                double t = 0.5/-z;
                double u = 0.5+t*y;
                double v = 0.5+t*x;
                if (u >= 0.0 && u <= 1.0 && v >= 0.0 && v <= 1.0) {
                    return get_pixel_from_uv(u, v, map.get("top"));
                }
            }
        }
        return null;
    }

    public Mat cubic2equirectangular(Map<String, Mat> map, String flip) {
        Mat item = map.get("top");
        Mat result = new Mat(item.rows() * 2, item.cols() * 4, item.type());
        for (int x = 0; x < result.cols(); ++x) {
            for (int y = 0; y < result.rows(); ++y) {
                double u = Float.valueOf(x)/Float.valueOf(result.cols());
                double v = Float.valueOf(y)/Float.valueOf(result.rows());
                double[] theta_phi = angular_position(new double[]{u, v});
                double[] pixel = pixel_value(theta_phi, map);
                if(null == pixel) continue;
                switch (flip) {
                    case "horizontal":
                        result.put(y, result.cols() - x, pixel);
                        break;
                    default:
                        result.put(y, x, pixel);
                }
            }
        }
        return result;
    }

}
