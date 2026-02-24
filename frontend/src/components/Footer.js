import React from 'react';
import { Container, Row, Col } from 'react-bootstrap';

const Footer = () => {
  return (
    <footer className="footer">
      <Container>
        <Row>
          <Col md={6}>
            <h5>E-Commerce Platform</h5>
            <p className="mb-0">
              A microservices demonstration project built with Spring Boot and React.js
            </p>
          </Col>
          <Col md={6} className="text-md-end">
            <h6>Quick Links</h6>
            <div className="d-flex flex-column">
              <a href="#products" className="text-light text-decoration-none mb-1">Products</a>
              <a href="#categories" className="text-light text-decoration-none mb-1">Categories</a>
              <a href="#admin" className="text-light text-decoration-none mb-1">Admin Panel</a>
            </div>
          </Col>
        </Row>
        <hr className="my-3" />
        <Row>
          <Col className="text-center">
            <p className="mb-0">
              &copy; 2024 E-Commerce Platform. Built for educational purposes.
            </p>
          </Col>
        </Row>
      </Container>
    </footer>
  );
};

export default Footer;
